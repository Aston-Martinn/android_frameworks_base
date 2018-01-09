/**
 * Copyright (C) 2017 The Android Open Source Project
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

package android.hardware.radio;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the RadioTuner interface by forwarding calls to radio service.
 */
class TunerAdapter extends RadioTuner {
    private static final String TAG = "BroadcastRadio.TunerAdapter";

    @NonNull private final ITuner mTuner;
    private boolean mIsClosed = false;

    private @RadioManager.Band int mBand;

    TunerAdapter(ITuner tuner, @RadioManager.Band int band) {
        if (tuner == null) {
            throw new NullPointerException();
        }
        mTuner = tuner;
        mBand = band;
    }

    @Override
    public void close() {
        synchronized (mTuner) {
            if (mIsClosed) {
                Log.v(TAG, "Tuner is already closed");
                return;
            }
            mIsClosed = true;
        }
        try {
            mTuner.close();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception trying to close tuner", e);
        }
    }

    @Override
    public int setConfiguration(RadioManager.BandConfig config) {
        if (config == null) return RadioManager.STATUS_BAD_VALUE;
        try {
            mTuner.setConfiguration(config);
            mBand = config.getType();
            return RadioManager.STATUS_OK;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Can't set configuration", e);
            return RadioManager.STATUS_BAD_VALUE;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
    }

    @Override
    public int getConfiguration(RadioManager.BandConfig[] config) {
        if (config == null || config.length != 1) {
            throw new IllegalArgumentException("The argument must be an array of length 1");
        }
        try {
            config[0] = mTuner.getConfiguration();
            return RadioManager.STATUS_OK;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
    }

    @Override
    public int setMute(boolean mute) {
        try {
            mTuner.setMuted(mute);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't set muted", e);
            return RadioManager.STATUS_ERROR;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
        return RadioManager.STATUS_OK;
    }

    @Override
    public boolean getMute() {
        try {
            return mTuner.isMuted();
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return true;
        }
    }

    @Override
    public int step(int direction, boolean skipSubChannel) {
        try {
            mTuner.step(direction == RadioTuner.DIRECTION_DOWN, skipSubChannel);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't step", e);
            return RadioManager.STATUS_INVALID_OPERATION;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
        return RadioManager.STATUS_OK;
    }

    @Override
    public int scan(int direction, boolean skipSubChannel) {
        try {
            mTuner.scan(direction == RadioTuner.DIRECTION_DOWN, skipSubChannel);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't scan", e);
            return RadioManager.STATUS_INVALID_OPERATION;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
        return RadioManager.STATUS_OK;
    }

    @Override
    public int tune(int channel, int subChannel) {
        try {
            mTuner.tune(ProgramSelector.createAmFmSelector(mBand, channel, subChannel));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't tune", e);
            return RadioManager.STATUS_INVALID_OPERATION;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Can't tune", e);
            return RadioManager.STATUS_BAD_VALUE;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
        return RadioManager.STATUS_OK;
    }

    @Override
    public void tune(@NonNull ProgramSelector selector) {
        try {
            mTuner.tune(selector);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public int cancel() {
        try {
            mTuner.cancel();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't cancel", e);
            return RadioManager.STATUS_INVALID_OPERATION;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
        return RadioManager.STATUS_OK;
    }

    @Override
    public void cancelAnnouncement() {
        try {
            mTuner.cancelAnnouncement();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public int getProgramInformation(RadioManager.ProgramInfo[] info) {
        if (info == null || info.length != 1) {
            throw new IllegalArgumentException("The argument must be an array of length 1");
        }
        try {
            info[0] = mTuner.getProgramInformation();
            return RadioManager.STATUS_OK;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return RadioManager.STATUS_DEAD_OBJECT;
        }
    }

    @Override
    public @Nullable Bitmap getMetadataImage(int id) {
        try {
            return mTuner.getImage(id);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public boolean startBackgroundScan() {
        try {
            return mTuner.startBackgroundScan();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public @NonNull List<RadioManager.ProgramInfo>
            getProgramList(@Nullable Map<String, String> vendorFilter) {
        try {
            return mTuner.getProgramList(vendorFilter);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public boolean isAnalogForced() {
        return isConfigFlagSet(RadioManager.CONFIG_FORCE_ANALOG);
    }

    @Override
    public void setAnalogForced(boolean isForced) {
        setConfigFlag(RadioManager.CONFIG_FORCE_ANALOG, isForced);
    }

    @Override
    public boolean isConfigFlagSupported(@RadioManager.ConfigFlag int flag) {
        try {
            return mTuner.isConfigFlagSupported(flag);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public boolean isConfigFlagSet(@RadioManager.ConfigFlag int flag) {
        try {
            return mTuner.isConfigFlagSet(flag);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public void setConfigFlag(@RadioManager.ConfigFlag int flag, boolean value) {
        try {
            mTuner.setConfigFlag(flag, value);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public @NonNull Map<String, String> setParameters(@NonNull Map<String, String> parameters) {
        try {
            return mTuner.setParameters(Objects.requireNonNull(parameters));
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public @NonNull Map<String, String> getParameters(@NonNull List<String> keys) {
        try {
            return mTuner.getParameters(Objects.requireNonNull(keys));
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public boolean isAntennaConnected() {
        try {
            return mTuner.isAntennaConnected();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    @Override
    public boolean hasControl() {
        try {
            // don't rely on mIsClosed, as tuner might get closed internally
            return !mTuner.isClosed();
        } catch (RemoteException e) {
            return false;
        }
    }
}
