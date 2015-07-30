package org.kotemaru.android.camera2sample;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.Face;
import android.media.FaceDetector;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener  {
	public int mis;
	SpeechRecognizer sr;
	Camera2StateMachine c2;
	// 音声合成用
	TextToSpeech tts = null;
	private AutoFitTextureView mTextureView;
	private ImageView mImageView;
	private Camera2StateMachine mCamera2;
	@
	Override

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		  c2=new Camera2StateMachine(this);
		setContentView(R.layout.activity_main);
		mTextureView = (AutoFitTextureView) findViewById(R.id.TextureView);
		mImageView = (ImageView) findViewById(R.id.ImageView);
		mCamera2 = new Camera2StateMachine(this);
		tts = new TextToSpeech(this, this);
	}


	@Override
	protected void onResume() {
		super.onResume();
		mCamera2.open(this, mTextureView);
	}


	@Override
	protected void onPause() {
		mCamera2.close();
		super.onPause();
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mImageView.getVisibility() == View.VISIBLE) {
			mTextureView.setVisibility(View.VISIBLE);
			mImageView.setVisibility(View.INVISIBLE);
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}


	public void speech(){
		tts.speak("こんにちは、今日は、オープンキャンパスにお越しいただきありがとうございます。", TextToSpeech.QUEUE_FLUSH, null);
	}


	public void onFace() {
		Log.d("KitCat", "onFace!");
		Intent intent = null;
		sr = SpeechRecognizer.createSpeechRecognizer(this);
		sr.setRecognitionListener(new MyRecognitionListener());

		// インテントを作成
		intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
		// 入力言語のロケールを設定
		//intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toString());
		//intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toString());
		// 音声認識APIにインテントを処理させる
		sr.startListening(intent);

		mCamera2.mIsTake = true;


/*		mCamera2.takePicture(new ImageReader.OnImageAvailableListener() {
			@Override
			public void onImageAvailable(ImageReader reader) {
				// 撮れた画像をImageViewに貼り付けて表示。
				final Image image = reader.acquireLatestImage();
				ByteBuffer buffer = image.getPlanes()[0].getBuffer();
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
				image.close();
				mImageView.setImageBitmap(bitmap);
				mImageView.setVisibility(View.VISIBLE);
				mTextureView.setVisibility(View.INVISIBLE);
			}
		});*/

	}

    public void onInit(int status) {
		if(status == TextToSpeech.SUCCESS) {
			// 音声合成の設定を行う
			float pitch = 1.0f; // 音の高低
			float rate = 1.0f; // 話すスピード
			Locale locale = Locale.US; // 対象言語のロケール
			tts.setPitch(pitch);
			tts.setSpeechRate(rate);
			tts.setLanguage(locale);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (tts != null) {
			// 破棄
			tts.shutdown();
		}
	}

	// 音声認識のリスナ

	class MyRecognitionListener implements RecognitionListener {
		@Override
		public void onBeginningOfSpeech() {
//			Log.d("KitCat", "onBeginningOfSpeech");
		}
		@Override
		public void onBufferReceived(byte[] buffer) {
//			Log.d("KitCat", "onBuffereReceived!");
		}
		@Override
		public void onEndOfSpeech() {
//			Log.d("KitCat", "onEndOfSpeech");
		}


		@Override
		public void onError(int error) {
			Log.d("KitCat", "onError");

			// 音声合成して発音
			if (tts.isSpeaking()) {

				mCamera2.mIsTake=false;
			}else{
				Random rnd = new Random();

				int ran = rnd.nextInt(5);

			//	tts.speak("ごめんなさい、なんて言っているのかわかりませんでした。", TextToSpeech.QUEUE_FLUSH, null);

		switch (ran){
				case 0:
				tts.speak("ごめんなさい、なんて言っているのかわかりませんでした。", TextToSpeech.QUEUE_FLUSH, null);
				break;
				case 1:
					tts.speak("うーん、なんていってるのかわかんないなあ", TextToSpeech.QUEUE_FLUSH, null);
				break;
			case 2:
				tts.speak("へー,そーなんだあ", TextToSpeech.QUEUE_FLUSH, null);
				break;
			case 3:
				tts.speak("そうだね、僕もそう思うよ", TextToSpeech.QUEUE_FLUSH, null);
				break;
			case 4:
				tts.speak("いいとおもいます", TextToSpeech.QUEUE_FLUSH, null);
				break;


				}
			}
			mCamera2.mIsTake = false;
//			onFace();

//			Toast.makeText(getApplicationContext(), "エラー： " + error, Toast.LENGTH_LONG).show();
		}
		@Override
		public void onEvent(int eventType, Bundle params) {
			Log.d("KitCat", "onEvent");
		}
		@Override
		public void onPartialResults(Bundle partialResults) {
			Log.d("KitCat", "onPartialResults!");
		}
		@Override
		public void onReadyForSpeech(Bundle params) {
			Log.d("KitCat", "onReadtForSpeech!");
			Toast.makeText(getApplicationContext(), "認識を開始します。", Toast.LENGTH_LONG).show();
		}

		@Override
		public void onResults(Bundle results) {
			Log.d("KitCat", "OnResult");
			// 結果を受け取る
			ArrayList<String> candidates = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			String s = candidates.get(0);
			for(int i = 0;i < candidates.size();i++)
			{
				Log.d("KitCat", "["+i+"] " + candidates.get(i));
			}
			// トーストで結果を表示
			Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
			HashMap<String , String> m = new HashMap<String , String>();


			m.put("こんにちは","こんにちは");
			m.put("名前を教えてください", "福山まさはるです、似てませんか？");
			m.put("好きな国はどこですか", "にっぽんです");
			m.put("好きな食べ物は何んですか", "へしこが大好きです");
			m.put("好きな子はいますか", "フォルテのバス停が気になります");
			m.put("好きな言葉は何ですか", "don’t settle for less ");
			m.put("同期の桜", "貴様と俺とは同期の桜, 同じ兵学校の庭に咲く, 咲いた花なら散るのは覚悟, 見事散りましょ国のため ");
			m.put("好きな場所は何処ですか", "ののいちし役所です");
			m.put("ゲームはしますか", "買ってください");
			m.put("何歳ですか", "二十歳ですやっと酒が飲めます,うぇーい");
			m.put("最近どうですか", "まあまあかな、そういえば台風きてるよね、家のまど閉めたかなあ");
			m.put("出身は何処ですか", "ののいち市です");
			m.put("出身はどこですか", "ののいち市です");

			m.put("特技は何ですか", "伸縮することだよ、すごいだろー");
			m.put("特技はなんですか", "伸縮することだよ、すごいだろー");

			m.put("趣味は何ですか", "べんきょうです");
			m.put("趣味はなんですか", "運動です");


			m.put("悩みは", "ダイエットしたいです");

			m.put("何処に住んでいるの", "ののいち市です");
			m.put("どこにすんでいるの", "ののいち市です");

			m.put("出身は何処ですか", "ののいち市です");
			m.put("出身は何処ですか", "ののいち市です");

			m.put("犬なの", "え、知らないの？、秘密だよー");
			m.put("職業は", "バスのうんえいです");


			m.put("いつも何してるの", "ばすにのってます");
			m.put("いつもなにしてるの", "ばすにのってます");


			m.put("夢は", "野々市市が有名になること");
			m.put("嫌いなことは何", "え、なんだろう");
			m.put("好きな食べ物は", "キウイフルーツが好きです");

			m.put("一番好きなものは何ですか", "みなさんの笑顔です");
			m.put("一番好きなものはなんですか", "みなさんの笑顔です");

			m.put("好きなサッカーチームは何処ですか", "つぇーげん金沢です");
			m.put("好きなサッカーチームはどこですか", "つぇーげん金沢です");

			m.put("好きな芸能人は誰ですか", "ローラーです");
			m.put("好きな芸能人はだれですか", "ローラーです");

			m.put("好きな動物は何ですか", "馬が大好きです");
			m.put("好きな動物はなにですか", "馬が大好きです");


			m.put("好きな飲み物は何ですか", "コーラがだいすきです。");
			m.put("好きな飲み物はなんですか", "コーラがだいすきです。");


			m.put("石川で好きなところはどこですか", "野々市市です");

			m.put("石川県で好きな場所は何処ですか", "野々市市です");

			m.put("石川で好きな所はどこですか", "野々市市です");



			m.put("好きなポケモンは何ですか", "ピカチュウです");
			m.put("好きなポケモンはなんですか", "ルキアです");

			m.put("好きな芸能人は誰ですか", "ローラーです");


			//	Log.d("main", m.get(s));
			// 音声合成して発音
			if(tts.isSpeaking()) {
				tts.stop();
			}
			if(m.get(s)!=null){
				tts.speak(m.get(s), TextToSpeech.QUEUE_FLUSH, null);
				mis =1;
			}else {
				tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
				mis = 1;
			}

			mCamera2.mIsTake=false;
		}
		@Override
		public void onRmsChanged(float rmsdB) {
//			Log.d("KitCat", "onRMSChange");
		}
	}
}
