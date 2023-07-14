/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */

package edu.jhuapl.trinity.utils.umap;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
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
 * #L%
 */

import java.util.ArrayList;
import java.util.List;


public final class UmapProgress {
    private static final UmapProgress PROGRESS = new UmapProgress();
    private static final long MIN_UPDATE_PERIOD = 500; // milliseconds

    private final List<ProgressListener> mProgressListeners = new ArrayList<>();
    private int mTotal = 0;
    private int mCounter = 0;
    private long mLastNotificationTime = 0L;

    private UmapProgress() {
    }

    public static synchronized void addProgressListener(final ProgressListener listener) {
        if (!PROGRESS.mProgressListeners.contains(listener)) {
            PROGRESS.mProgressListeners.add(listener);
        }
    }

    public static synchronized boolean removeProgressListener(final ProgressListener listener) {
        return PROGRESS.mProgressListeners.remove(listener);
    }

    protected void notifyListeners(ProgressState state) {
        // limit calls to notify if occurring too often
        final long now = System.currentTimeMillis();
        if (now - mLastNotificationTime > MIN_UPDATE_PERIOD) {
            for (final ProgressListener listener : mProgressListeners) {
                listener.updated(state);
            }
            mLastNotificationTime = now;
        }
    }

    public static synchronized void reset(final int total) {
        PROGRESS.mTotal = total;
        PROGRESS.mCounter = 0;
        PROGRESS.mLastNotificationTime = 0L;
        update(0);
    }

    public static synchronized void incTotal(final int inc) {
        PROGRESS.mTotal += inc;
        update(0);
    }

    public static synchronized void finished() {
        PROGRESS.mCounter = PROGRESS.mTotal;
        PROGRESS.mLastNotificationTime = 0L;
        update(0);
    }

    public static void update() {
        update(1);
    }

    public static synchronized void update(int n) {
        PROGRESS.mCounter += n;
        if (PROGRESS.mCounter > PROGRESS.mTotal) {
            Utils.message("Update counter exceeded total: " + PROGRESS.mCounter + " : " + PROGRESS.mTotal);
        }
        PROGRESS.notifyListeners(getProgress());
    }

    public static ProgressState getProgress() {
        return new ProgressState(PROGRESS.mTotal, PROGRESS.mCounter);
    }
}
