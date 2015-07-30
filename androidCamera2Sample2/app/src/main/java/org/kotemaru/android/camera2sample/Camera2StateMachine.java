// Copyright 2015 kotemaru.org. (http://www.apache.org/licenses/LICENSE-2.0)
package org.kotemaru.android.camera2sample;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

public class Camera2StateMachine {
	public int fps=0;
	public int count=0;


	public boolean mIsTake = false;

	MainActivity context=null;


	public Camera2StateMachine(MainActivity c){
		context=c;

	}
	private static final String TAG = Camera2StateMachine.class.getSimpleName();
	private CameraManager mCameraManager;

	private CameraDevice mCameraDevice;
	private CameraCaptureSession mCaptureSession;
	private ImageReader mImageReader;
	private CaptureRequest.Builder mPreviewRequestBuilder;

	private AutoFitTextureView mTextureView;
	private Handler mHandler = null; // default current thread.
	private State mState = null;
	private ImageReader.OnImageAvailableListener mTakePictureListener;

	public void open(Activity activity, AutoFitTextureView textureView) {
		if (mState != null) throw new IllegalStateException("Alrady started state=" + mState);
		mTextureView = textureView;
		mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
		nextState(mInitSurfaceState);
	}
	public boolean takePicture(ImageReader.OnImageAvailableListener listener) {
		if (mState != mPreviewState) return false;
		mTakePictureListener = listener;
		nextState(mAutoFocusState);
		return true;
	}
	public void close() {
		nextState(mAbortState);
	}
	// ----------------------------------------------------------------------------------------
	// The following private
	private void shutdown() {
		if (null != mCaptureSession) {
			mCaptureSession.close();
			mCaptureSession = null;
		}
		if (null != mCameraDevice) {
			mCameraDevice.close();
			mCameraDevice = null;
		}
		if (null != mImageReader) {
			mImageReader.close();
			mImageReader = null;
		}
	}
	private void nextState(State nextState) {
		Log.d(TAG, "state: " + mState + "->" + nextState);
		try {
			if (mState != null) mState.finish();
			mState = nextState;
			if (mState != null) mState.enter();
		} catch (CameraAccessException e) {
			Log.e(TAG, "next(" + nextState + ")", e);
			shutdown();
		}
	}
	private abstract class State {
		private String mName;
		public State(String name) {
			mName = name;
		}
		//@formatter:off
		public String toString() {return mName;}
		public void enter() throws CameraAccessException {}
		public void onSurfaceTextureAvailable(int width, int height){}
		public void onCameraOpened(CameraDevice cameraDevice){}
		public void onSessionConfigured(CameraCaptureSession cameraCaptureSession) {}
		public void onCaptureResult(CaptureResult result, boolean isCompleted) throws CameraAccessException {}
		public void finish() throws CameraAccessException {}
		//@formatter:on
	}
	// ===================================================================================
	// State Definition
	private final State mInitSurfaceState = new State("InitSurface") {
		public void enter() throws CameraAccessException {
			if (mTextureView.isAvailable()) {
				nextState(mOpenCameraState);
			} else {
				mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
			}
		}
		public void onSurfaceTextureAvailable(int width, int height) {
			nextState(mOpenCameraState);
		}
		private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
				if (mState != null) mState.onSurfaceTextureAvailable(width, height);
			}
			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
				// TODO: ratation changed.
			}
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
				return true;
			}
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture texture) {

			}
		};
	};

	// -----------------------------------------------------------------------------------

	private final State mOpenCameraState = new State("OpenCamera") {
		public void enter() throws CameraAccessException {
			//configureTransform(width, height);
			String cameraId = Camera2Util.getCameraId(mCameraManager, CameraCharacteristics.LENS_FACING_FRONT);
			CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			mImageReader = Camera2Util.getMaxSizeImageReader(map, ImageFormat.JPEG);
			Size previewSize = Camera2Util.getBestPreviewSize(map, mImageReader);
			mTextureView.setPreviewSize(previewSize.getHeight(), previewSize.getWidth());
			mCameraManager.openCamera(cameraId, mStateCallback, mHandler);
			Log.d(TAG, "openCamera:" + cameraId);
		}

		public void onCameraOpened(CameraDevice cameraDevice) {
			mCameraDevice = cameraDevice;
			nextState(mCreateSessionState);
		}

		private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
			@Override
			public void onOpened(CameraDevice cameraDevice) {
				if (mState != null) mState.onCameraOpened(cameraDevice);
			}
			@Override
			public void onDisconnected(CameraDevice cameraDevice) {
				nextState(mAbortState);
			}
			@Override
			public void onError(CameraDevice cameraDevice, int error) {
				Log.e(TAG, "CameraDevice:onError:" + error);
				nextState(mAbortState);
			}
		};
	};

	// -----------------------------------------------------------------------------------

	private final State mCreateSessionState = new State("CreateSession") {
		public void enter() throws CameraAccessException {
			mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			texture.setDefaultBufferSize(mTextureView.getPreviewWidth(), mTextureView.getPreviewHeight());
			Surface surface = new Surface(texture);
			mPreviewRequestBuilder.addTarget(surface);
			List<Surface> outputs = Arrays.asList(surface, mImageReader.getSurface());
			mCameraDevice.createCaptureSession(outputs, mSessionCallback, mHandler);

		}

		public void onSessionConfigured(CameraCaptureSession cameraCaptureSession) {
			mCaptureSession = cameraCaptureSession;
			nextState(mPreviewState);
		}
		private final CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
			@Override
			public void onConfigured(CameraCaptureSession cameraCaptureSession) {
				if (mState != null) mState.onSessionConfigured(cameraCaptureSession);
			}
			@Override
			public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
				nextState(mAbortState);
			}
		};
	};
	// -----------------------------------------------------------------------------------
	private final State mPreviewState = new State("Preview") {
		public void enter() throws CameraAccessException {
			mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
			mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mHandler);

		}
	};

	public final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
		@Override
		public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
			onCaptureResult(partialResult, false);
		}

		private int noFaceCount = 0;
		@Override
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
			Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
			if(faces.length > 0) {
				if(!mIsTake) {
//					mIsTake = true;
					if (!context.tts.isSpeaking()) {        // 音声合成して発音
						if (fps == 0) {
							Random rnd2 = new Random();

							int gree = rnd2.nextInt(4);
							switch (gree){
								case 0:
									context.tts.speak("こんにちは、今日は、オープンキャンパスにお越しいただきありがとうございます。", TextToSpeech.QUEUE_FLUSH, null);
 										break;
								case 1:
									context.tts.speak(" 八束穂へようこそ！、今日は、来てくれて,ありがとう", TextToSpeech.QUEUE_FLUSH, null);
									break;
								case 2:
									context.tts.speak("いらっしゃい、なにかはなそうよ", TextToSpeech.QUEUE_FLUSH, null);
									break;
								case 3:
									context.tts.speak("ハロー、みんな元気？", TextToSpeech.QUEUE_FLUSH, null);
									break;
								case 4:
									context.tts.speak("こんにちは、のってィです。", TextToSpeech.QUEUE_FLUSH, null);
									break;
								case 5:
									context.tts.speak("気軽に、話しかけてね？", TextToSpeech.QUEUE_FLUSH, null);
									break;

							}

							fps = 1;
						} else{
							context.onFace();
						}
					}
				}
				noFaceCount = 0;
			} else {
				noFaceCount++;
				if(noFaceCount > 400)
				{
					fps = 0;
				}
			}
			Log.d("NOFACE","NFC:"+noFaceCount);
			/*
			if(!context.tts.isSpeaking() && !context.isListening){
				Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
				if(faces.length > 0) {
					// 音声合成して発音
					if (fps == 0) {
						Log.d("KitCat","before speak");
						context.tts.speak("こんにちは、今日は、オープンキャンパスにお越しいただきありがとうございます。", TextToSpeech.QUEUE_FLUSH, null);
						Log.d("KitCat","after speak");
						fps=1;
					}else{
						Log.d("KitCat","before onface");
						context.onFace();
						Log.d("KitCat", "after onface");
						context.isListening = true;
					}
				}else {
					fps=0;
					faceResestCount = 0;
					Log.d("KitCat","New Face1");
				}
				onCaptureResult(result, true);
			}else
			{
				if(context.isListening)
				{
//					Log.d("KitCat",""+faceResestCount);
					faceResestCount++;
					if(faceResestCount > 500)
					{
						context.isListening = false;
						fps=0;
						Log.d("KitCat","New Face2");
						faceResestCount = 0;
					}
					Log.d("KitCat", "Listening now" + faceResestCount);

				}else
				{
					Log.d("KitCat", "Speaking now");
					faceResestCount = 0;
				}
			}
			*/
			}




		private void onCaptureResult(CaptureResult result, boolean isCompleted) {
			try {
				if (mState != null) mState.onCaptureResult(result, isCompleted);
			} catch (CameraAccessException e) {
				Log.e(TAG, "handle():", e);
				nextState(mAbortState);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};





	// -----------------------------------------------------------------------------------
	private final State mAutoFocusState = new State("AutoFocus") {
		public void enter() throws CameraAccessException {
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
			mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mHandler);
		}
		public void onCaptureResult(CaptureResult result, boolean isCompleted) throws CameraAccessException {
			Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
			boolean isAfReady = afState == null
					|| afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
					|| afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED;
			if (isAfReady) {
				nextState(mAutoExposureState);
			}
		}
	};
	// -----------------------------------------------------------------------------------
	private final State mAutoExposureState = new State("AutoExposure") {
		public void enter() throws CameraAccessException {
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
					CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
			mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mHandler);
		}
		public void onCaptureResult(CaptureResult result, boolean isCompleted) throws CameraAccessException {
			Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
			boolean isAeReady = aeState == null
					|| aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
					|| aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED;
			if (isAeReady) {
				nextState(mTakePictureState);
			}
		}
	};

	// -----------------------------------------------------------------------------------

	private final State mTakePictureState = new State("TakePicture") {
		public void enter() throws CameraAccessException {
			final CaptureRequest.Builder captureBuilder =
					mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureBuilder.addTarget(mImageReader.getSurface());
			captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
			captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90); // portraito
			mImageReader.setOnImageAvailableListener(mTakePictureListener, mHandler);

			mCaptureSession.stopRepeating();
			mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, mHandler);
		}
		public void onCaptureResult(CaptureResult result, boolean isCompleted) throws CameraAccessException {
			if (isCompleted) {
				nextState(mPreviewState);
			}
		}

		public void finish() throws CameraAccessException {
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
			mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mHandler);
			mTakePictureListener = null;
		}
	};

	// -----------------------------------------------------------------------------------

	private final State mAbortState = new State("Abort") {
		public void enter() throws CameraAccessException {
			shutdown();
			nextState(null);
		}
	};
}
