package com.journaldev.facedetectionapi;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
    @Override
    public Tracker<Face> create(Face face) {

        return null;
    }
}
