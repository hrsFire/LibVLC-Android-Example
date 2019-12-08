package com.example.libvlcexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String ASSET_FILENAME = "bbb.mp4";
    private TextureView videoTexture;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;

    private Paint paint;

    private ConstraintLayout annotationContainer;
    private RelativeLayout.LayoutParams defaultAnnotationContainerParams;
    private RelativeLayout.LayoutParams stretchedAnnotationContainerParams;
    private View topMenuBar, bottomMenuBar;
    private ImageView annotationBackgroundView;
    private ImageView annotationCanvasView;
    private View videoModeToggleButton;
    private View playButton;
    private View trashButton;
    private boolean isStretchedFullscreenActive;

    private boolean hasAnnotationStarted;
    private Bitmap currentBackground;
    private Bitmap canvasBitmap;
    private Path currentPath;
    private List<Path> strokes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_main);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        libVLC = new LibVLC(this, args);
        mediaPlayer = new MediaPlayer(libVLC);

        initializeView();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mediaPlayer.stop();
        mediaPlayer.detachViews();

        mediaPlayer.setEventListener(null);
    }

    private void initializeView() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        videoTexture = findViewById(R.id.video_texture);
        annotationContainer = findViewById(R.id.annotation_container);
        topMenuBar = findViewById(R.id.top_menu_bar);
        bottomMenuBar = findViewById(R.id.bottom_menu_bar);
        annotationBackgroundView = findViewById(R.id.annotation_background_view);
        annotationCanvasView = findViewById(R.id.annotation_canvas_view);
        videoModeToggleButton = findViewById(R.id.video_mode_toggle_btn);
        playButton = findViewById(R.id.play_btn);
        trashButton = findViewById(R.id.trash_btn);

        videoTexture.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (strokes != null) {
                    float oldWidth = oldRight - oldLeft;
                    float oldHeight = oldBottom - oldTop;
                    float width = right - left;
                    float height = bottom - top;

                    float widthScale = width / oldWidth;
                    float heightScale = height / oldHeight;

                    Matrix transformationMatrix = new Matrix();
                    transformationMatrix.setScale(widthScale, heightScale);

                    for (Iterator<Path> iterator = strokes.iterator(); iterator.hasNext();) {
                        Path path = iterator.next();

                        path.transform(transformationMatrix);
                    }
                }
            }
        });

        videoTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mediaPlayer.getVLCVout().setVideoSurface(surface);
                // Resize the video player to fit the space which was assigned by the constrained layout.
                mediaPlayer.getVLCVout().setWindowSize(width, height);

                mediaPlayer.getVLCVout().attachViews();
                useDefaultAspectRatio();
                playVideo();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                mediaPlayer.getVLCVout().setWindowSize(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        annotationBackgroundView.setOnTouchListener(new DoubleTouchListener() {
            @Override
            public void onMotion(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println("switch: ACTION_DOWN");

                        if (!hasAnnotationStarted) {
                            startAnnotation();
                        }

                        currentPath = new Path();
                        strokes.add(currentPath);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        System.out.println("switch: ACTION_MOVE");

                        if (currentPath.isEmpty()) {
                            currentPath.addRect(x, y, x, y, Path.Direction.CCW);
                        } else {
                            currentPath.lineTo(x, y);
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        System.out.println("switch: ACTION_UP");
                        currentPath = null;

                        break;
                    default:
                        System.out.println("switch: DEFAULT");
                        break;
                }

                if (hasAnnotationStarted) {
                    draw();
                }
            }

            @Override
            public void onDoubleClick(View v) {
                toggleMenuBars();
            }
        });

        videoModeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVideoModes();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAnnotation();
            }
        });

        trashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAnnotation();
            }
        });

        trashButton.setEnabled(hasAnnotationStarted);
        playButton.setEnabled(hasAnnotationStarted);

        defaultAnnotationContainerParams = (RelativeLayout.LayoutParams) annotationContainer.getLayoutParams();
        stretchedAnnotationContainerParams = new RelativeLayout.LayoutParams(defaultAnnotationContainerParams);
        stretchedAnnotationContainerParams.removeRule(RelativeLayout.ABOVE);
        stretchedAnnotationContainerParams.removeRule(RelativeLayout.BELOW);
    }

    private void toggleMenuBars() {
        if (topMenuBar.getAnimation() == null && bottomMenuBar.getAnimation() == null) {
            final Animation topMenuBarAnimation;
            final Animation bottomMenuBarAnimation;
            final int visibility;
            int duration = 1500;
            int topMenuBarToYDelta, bottomMenuBarToYDelta;
            int topMenuBarFromYDelta, bottomMenuBarFromYDelta;

            if (bottomMenuBar.getVisibility() == View.VISIBLE) {
                topMenuBarFromYDelta = bottomMenuBarFromYDelta = 0;
                topMenuBarToYDelta = -topMenuBar.getHeight();
                bottomMenuBarToYDelta = bottomMenuBar.getHeight();
                visibility = View.INVISIBLE;
            } else {
                topMenuBarFromYDelta = -topMenuBar.getHeight();
                bottomMenuBarFromYDelta = bottomMenuBar.getHeight();
                topMenuBarToYDelta = bottomMenuBarToYDelta = 0;
                visibility = View.VISIBLE;
            }

            // create the animations
            topMenuBarAnimation = new TranslateAnimation(0, 0, topMenuBarFromYDelta, topMenuBarToYDelta);
            topMenuBarAnimation.setDuration(duration);

            topMenuBarAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    topMenuBar.setVisibility(visibility);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            bottomMenuBarAnimation = new TranslateAnimation(0, 0, bottomMenuBarFromYDelta, bottomMenuBarToYDelta);
            bottomMenuBarAnimation.setDuration(duration);

            bottomMenuBarAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    bottomMenuBar.setVisibility(visibility);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            topMenuBar.startAnimation(topMenuBarAnimation);
            bottomMenuBar.startAnimation(bottomMenuBarAnimation);
        }
    }

    private void toggleVideoModes() {
        if (isStretchedFullscreenActive) {
            annotationContainer.setLayoutParams(defaultAnnotationContainerParams);
            useStretchedAspectRatio();
        } else {
            annotationContainer.setLayoutParams(stretchedAnnotationContainerParams);
            useDefaultAspectRatio();
        }

        isStretchedFullscreenActive = !isStretchedFullscreenActive;
    }

    private void startAnnotation() {
        currentBackground = takeSnapshot();
        annotationBackgroundView.setImageBitmap(currentBackground);
        hasAnnotationStarted = true;
        strokes = new LinkedList<>();
        trashButton.setEnabled(hasAnnotationStarted);
        playButton.setEnabled(hasAnnotationStarted);
    }

    private void stopAnnotation() {
        hasAnnotationStarted = false;
        canvasBitmap = null;
        currentBackground = null;
        strokes = null;
        currentPath = null;
        Bitmap transparentBitmap = Bitmap.createBitmap(annotationCanvasView.getWidth(), annotationCanvasView.getHeight(), Bitmap.Config.ARGB_8888);
        annotationBackgroundView.setImageBitmap(transparentBitmap);
        annotationCanvasView.setImageBitmap(transparentBitmap);
        trashButton.setEnabled(hasAnnotationStarted);
        playButton.setEnabled(hasAnnotationStarted);
    }

    private void clearAnnotation() {
        strokes = new LinkedList<>();
        canvasBitmap.eraseColor(Color.TRANSPARENT);
    }

    private void draw() {
        Bitmap resultBitmap;

        if (canvasBitmap == null) {
            resultBitmap = Bitmap.createBitmap(currentBackground);
            resultBitmap.eraseColor(Color.TRANSPARENT);
        } else {
            resultBitmap = Bitmap.createScaledBitmap(canvasBitmap, annotationBackgroundView.getWidth(), annotationBackgroundView.getHeight(), false);
        }

        Canvas canvas = new Canvas(resultBitmap);

        for (Path path : strokes) {
            canvas.drawPath(path, paint);
        }

        canvasBitmap = resultBitmap;
        annotationCanvasView.setImageBitmap(resultBitmap);
    }

    // Vlc media player specific

    private Media getVideo() {
        Media media;

        try {
            media = new Media(libVLC, getAssets().openFd(ASSET_FILENAME));

        } catch (IOException e) {
            throw new RuntimeException("Invalid asset folder");
        }

        return media;
    }

    private void useDefaultAspectRatio() {
        mediaPlayer.setAspectRatio("16:9");
    }

    private void useStretchedAspectRatio() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        mediaPlayer.setAspectRatio(displayMetrics.widthPixels + ":" + displayMetrics.heightPixels);
    }

    private void playVideo() {
        Media media = getVideo();
        mediaPlayer.setMedia(media);
        media.release();

        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                if (event.type == MediaPlayer.Event.Stopped) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Media media = getVideo();
                            mediaPlayer.setMedia(media);
                            media.release();
                            mediaPlayer.play();
                        }
                    });
                }
            }
        });

        mediaPlayer.play();
    }

    private Bitmap takeSnapshot() {
        return videoTexture.getBitmap();
    }
}