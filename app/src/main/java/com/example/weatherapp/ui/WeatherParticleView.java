package com.example.weatherapp.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.weatherapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class WeatherParticleView extends View {

    private static final int PARTICLE_COUNT = 48;

    private final Random random = new Random();
    private final Paint rainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint snowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Particle> particles = new ArrayList<>();

    private final int rainColor;
    private final int snowColor;
    private final int sunColor;
    private final int cloudColor;

    private ValueAnimator animator;
    private long lastFrameTimeNs;
    private Mode mode = Mode.CLOUDS;

    public WeatherParticleView(Context context) {
        this(context, null);
    }

    public WeatherParticleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeatherParticleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        rainColor = ContextCompat.getColor(context, R.color.particle_rain);
        snowColor = ContextCompat.getColor(context, R.color.particle_snow);
        sunColor = ContextCompat.getColor(context, R.color.particle_sun);
        cloudColor = ContextCompat.getColor(context, R.color.particle_cloud);

        rainPaint.setStrokeCap(Paint.Cap.ROUND);
        rainPaint.setStrokeWidth(4f);
        rainPaint.setColor(rainColor);

        snowPaint.setStyle(Paint.Style.FILL);
        snowPaint.setColor(snowColor);

        sunPaint.setStyle(Paint.Style.STROKE);
        sunPaint.setStrokeCap(Paint.Cap.ROUND);
        sunPaint.setStrokeWidth(3f);
        sunPaint.setColor(sunColor);

        sparklePaint.setStyle(Paint.Style.FILL);
        sparklePaint.setColor(sunColor);

        cloudPaint.setStyle(Paint.Style.FILL);
        cloudPaint.setColor(cloudColor);

        startAnimator();
    }

    public void setWeatherCondition(String condition) {
        String normalized = condition == null
                ? ""
                : condition.toLowerCase(Locale.ROOT);

        if (normalized.contains("snow") || normalized.contains("χιο")) {
            mode = Mode.SNOW;
        } else if (normalized.contains("rain")
                || normalized.contains("drizzle")
                || normalized.contains("shower")
                || normalized.contains("storm")
                || normalized.contains("βρο")
                || normalized.contains("καταιγ")) {
            mode = Mode.RAIN;
        } else if (normalized.contains("clear")
                || normalized.contains("sun")
                || normalized.contains("fair")
                || normalized.contains("ηλιο")
                || normalized.contains("αίθρ")) {
            mode = Mode.CLEAR;
        } else {
            mode = Mode.CLOUDS;
        }

        buildParticles(getWidth(), getHeight());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildParticles(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Particle particle : particles) {
            switch (mode) {
                case RAIN:
                    drawRain(canvas, particle);
                    break;
                case SNOW:
                    drawSnow(canvas, particle);
                    break;
                case CLEAR:
                    drawSparkle(canvas, particle);
                    break;
                case CLOUDS:
                default:
                    drawCloud(canvas, particle);
                    break;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator == null) {
            startAnimator();
        } else if (!animator.isRunning()) {
            lastFrameTimeNs = 0L;
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        lastFrameTimeNs = 0L;
        super.onDetachedFromWindow();
    }

    private void startAnimator() {
        lastFrameTimeNs = 0L;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1200L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            long now = System.nanoTime();
            if (lastFrameTimeNs == 0L) {
                lastFrameTimeNs = now;
            }
            float deltaSeconds = (now - lastFrameTimeNs) / 1_000_000_000f;
            lastFrameTimeNs = now;
            updateParticles(deltaSeconds);
            postInvalidateOnAnimation();
        });
        animator.start();
    }

    private void buildParticles(int width, int height) {
        particles.clear();
        if (width <= 0 || height <= 0) {
            return;
        }

        int count = mode == Mode.CLOUDS ? 24 : PARTICLE_COUNT;
        for (int i = 0; i < count; i++) {
            particles.add(createParticle(width, height));
        }
    }

    private Particle createParticle(int width, int height) {
        Particle particle = new Particle();
        particle.x = random.nextFloat() * width;
        particle.y = random.nextFloat() * height;
        particle.size = 8f + random.nextFloat() * 18f;
        particle.speed = 22f + random.nextFloat() * 220f;
        particle.alpha = 0.25f + random.nextFloat() * 0.65f;
        particle.phase = random.nextFloat() * ((float) Math.PI * 2f);
        particle.sway = 12f + random.nextFloat() * 40f;
        return particle;
    }

    private void updateParticles(float deltaSeconds) {
        if (particles.isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        for (Particle particle : particles) {
            particle.phase += deltaSeconds * (0.8f + particle.speed / 180f);
            switch (mode) {
                case RAIN:
                    particle.y += particle.speed * deltaSeconds;
                    particle.x += 18f * deltaSeconds;
                    break;
                case SNOW:
                    particle.y += (particle.speed * 0.35f) * deltaSeconds;
                    particle.x += (float) Math.sin(particle.phase) * particle.sway * deltaSeconds;
                    break;
                case CLEAR:
                    particle.y += (particle.speed * 0.08f) * deltaSeconds;
                    particle.x += (float) Math.cos(particle.phase) * 10f * deltaSeconds;
                    break;
                case CLOUDS:
                default:
                    particle.x += (particle.speed * 0.08f) * deltaSeconds;
                    particle.y += (float) Math.sin(particle.phase) * 4f * deltaSeconds;
                    break;
            }

            if (particle.y > height + particle.size * 3f) {
                particle.y = -particle.size * 3f;
                particle.x = random.nextFloat() * width;
            }
            if (particle.x > width + particle.size * 4f) {
                particle.x = -particle.size * 4f;
            } else if (particle.x < -particle.size * 4f) {
                particle.x = width + particle.size * 4f;
            }
        }
    }

    private void drawRain(Canvas canvas, Particle particle) {
        rainPaint.setAlpha((int) (255 * particle.alpha));
        float endX = particle.x + particle.size * 0.4f;
        float endY = particle.y + particle.size * 2.4f;
        canvas.drawLine(particle.x, particle.y, endX, endY, rainPaint);
    }

    private void drawSnow(Canvas canvas, Particle particle) {
        snowPaint.setColor(snowColor);
        snowPaint.setAlpha((int) (255 * particle.alpha));
        canvas.drawCircle(particle.x, particle.y, particle.size * 0.3f, snowPaint);
    }

    private void drawSparkle(Canvas canvas, Particle particle) {
        float twinkle = 0.45f + 0.55f * (float) Math.abs(Math.sin(particle.phase * 1.6f));
        sunPaint.setAlpha((int) (255 * twinkle));
        float radius = particle.size * 0.4f;
        canvas.drawLine(particle.x - radius, particle.y, particle.x + radius, particle.y, sunPaint);
        canvas.drawLine(particle.x, particle.y - radius, particle.x, particle.y + radius, sunPaint);
        sparklePaint.setAlpha((int) (255 * twinkle));
        canvas.drawCircle(particle.x, particle.y, radius * 0.3f, sparklePaint);
    }

    private void drawCloud(Canvas canvas, Particle particle) {
        cloudPaint.setAlpha((int) (140 * particle.alpha));
        float radius = particle.size;
        canvas.drawCircle(particle.x, particle.y, radius, cloudPaint);
        canvas.drawCircle(particle.x + radius * 0.8f, particle.y - radius * 0.2f, radius * 0.85f, cloudPaint);
        canvas.drawCircle(particle.x - radius * 0.7f, particle.y + radius * 0.1f, radius * 0.75f, cloudPaint);
    }

    private enum Mode {
        RAIN,
        SNOW,
        CLEAR,
        CLOUDS
    }

    private static class Particle {
        float x;
        float y;
        float size;
        float speed;
        float alpha;
        float phase;
        float sway;
    }
}
