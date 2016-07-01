/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dreams.basic;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import cyanogenmod.externalviews.KeyguardExternalViewProviderService;

/**
 * Plays a delightful show of colors.
 * <p>
 * This dream performs its rendering using OpenGL on a separate rendering thread.
 * </p>
 */
public class ColorsLockScreen extends KeyguardExternalViewProviderService {
    static final String TAG = ColorsLockScreen.class.getSimpleName();
    static final boolean DEBUG = false;

    public static void LOG(String fmt, Object... args) {
        if (!DEBUG) return;
        Log.v(TAG, String.format(fmt, args));
    }

    @Override
    protected Provider createExternalView(Bundle options) {
        return new ProviderImpl(options);
    }

    private class ProviderImpl extends Provider implements TextureView.SurfaceTextureListener {
        private TextureView mTextureView;

        // The handler thread and handler on which the GL renderer is running.
        private HandlerThread mRendererHandlerThread;
        private Handler mRendererHandler;

        // The current GL renderer, or null if the dream is not running.
        private ColorsGLRenderer mRenderer;

        protected ProviderImpl(Bundle options) {
            super(options);
        }

        @Override
        protected View onCreateView() {
            mTextureView = new TextureView(ColorsLockScreen.this);
            mTextureView.setSurfaceTextureListener(this);

            return mTextureView;
        }

        @Override
        protected void onKeyguardShowing(boolean screenOn) {
            resumeRendering();
        }

        @Override
        protected void onKeyguardDismissed() {
            pauseRendering();
        }

        @Override
        protected void onBouncerShowing(boolean showing) {
        }

        @Override
        protected void onScreenTurnedOn() {
            resumeRendering();
        }

        @Override
        protected void onScreenTurnedOff() {
            pauseRendering();
        }

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surface,
                final int width, final int height) {
            LOG("onSurfaceTextureAvailable(%s, %d, %d)", surface, width, height);

            if (mRendererHandlerThread == null) {
                mRendererHandlerThread = new HandlerThread(TAG);
                mRendererHandlerThread.start();
                mRendererHandler = new Handler(mRendererHandlerThread.getLooper());
            }

            mRendererHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRenderer != null) {
                        mRenderer.stop();
                    }
                    mRenderer = new ColorsGLRenderer(surface, width, height, 1 / 3000f);
                    mRenderer.start();
                }
            });
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                final int width, final int height) {
            LOG("onSurfaceTextureSizeChanged(%s, %d, %d)", surface, width, height);

            mRendererHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRenderer != null) {
                        mRenderer.setSize(width, height);
                    }
                }
            });
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            LOG("onSurfaceTextureDestroyed(%s)", surface);

            mRendererHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRenderer != null) {
                        mRenderer.stop();
                        mRenderer = null;
                    }
                    mRendererHandlerThread.quit();
                    mRendererHandlerThread = null;
                }
            });

            try {
                if (mRendererHandlerThread != null) mRendererHandlerThread.join();
            } catch (InterruptedException e) {
                LOG("Error while waiting for renderer", e);
            }

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            LOG("onSurfaceTextureUpdated(%s)", surface);
        }

        private void pauseRendering() {
            if (mRenderer != null) {
                mRendererHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRenderer.pause();
                    }
                });
            }
        }

        private void resumeRendering() {
            if (mRenderer != null) {
                mRendererHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRenderer.resume();
                    }
                });
            }
        }
    }
}
