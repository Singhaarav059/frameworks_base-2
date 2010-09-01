/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.dumprendertree2;

import android.os.Environment;
import android.os.Message;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A Thread that is responsible for generating a lists of tests to run.
 */
public class TestsListPreloaderThread extends Thread {

    private static final String LOG_TAG = "TestsListPreloaderThread";

    /** TODO: make it a setting */
    private static final String TESTS_ROOT_DIR_PATH =
            Environment.getExternalStorageDirectory() +
            File.separator + "android" +
            File.separator + "LayoutTests";

    /** A list containing relative paths of tests to run */
    private ArrayList<String> mTestsList = new ArrayList<String>();

    private FileFilter mFileFilter;

    /**
     * A relative path to the folder with the tests we want to run or particular test.
     * Used up to and including preloadTests().
     */
    private String mRelativePath;

    private Message mDoneMsg;

    /**
     * The given path must be relative to the root dir.
     *
     * @param path
     * @param doneMsg
     */
    public TestsListPreloaderThread(String path, Message doneMsg) {
        mFileFilter = new FileFilter(TESTS_ROOT_DIR_PATH);
        mRelativePath = path;
        mDoneMsg = doneMsg;
    }

    @Override
    public void run() {
        if (FileFilter.isTestFile(mRelativePath)) {
            mTestsList.add(mRelativePath);
        } else {
            loadTestsFromUrl(mRelativePath);
        }

        mDoneMsg.obj = mTestsList;
        mDoneMsg.sendToTarget();
    }

    /**
     * Loads all the tests from the given folders and all the subfolders
     * into mTestsList.
     *
     * @param dirRelativePath
     */
    private void loadTestsFromUrl(String dirRelativePath) {
        LinkedList<String> foldersList = new LinkedList<String>();
        foldersList.add(dirRelativePath);

        String relativePath;
        String itemName;
        while (!foldersList.isEmpty()) {
            relativePath = foldersList.removeFirst();

            for (String folderRelativePath : FsUtils.getLayoutTestsDirContents(relativePath,
                    false, true)) {
                itemName = new File(folderRelativePath).getName();
                if (FileFilter.isTestDir(itemName)) {
                    foldersList.add(folderRelativePath);
                }
            }

            for (String testRelativePath : FsUtils.getLayoutTestsDirContents(relativePath,
                    false, false)) {
                itemName = new File(testRelativePath).getName();
                if (FileFilter.isTestFile(itemName)) {
                    if (!mFileFilter.isSkip(testRelativePath)) {
                        mTestsList.add(testRelativePath);
                    } else {
                        //mSummarizer.addSkippedTest(relativePath);
                        /** TODO: Summarizer is now in service - figure out how to send the info */
                    }
                }
            }
        }
    }
}