package surajama.droiddrop;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.Collection;
import java.util.Random;


/**
 * Our WalkieTalkie Activity. This Activity has 4 {@link State}s.
 *
 * <p>{@link State#UNKNOWN}: We cannot do anything. We're waiting for the GoogleApiClient to
 * connect.
 *
 * <p>{@link State#DISCOVERING}: Our default state (after we've connected). We constantly listen for
 * a device to advertise near us.
 *
 * <p>{@link State#ADVERTISING}: If a user shakes their device, they enter this state. We advertise
 * our device so that others nearby can discover us.
 *
 * <p>{@link State#CONNECTED}: We've connected to another device. We can now talk to them by holding
 * down the volume keys and speaking into the phone. We'll continue to advertise (if we were already
 * advertising) so that more people can connect to us.
 */
public class MainActivity extends Activity {
  /** If true, debug logs are shown on the device. */
  private static final boolean DEBUG = true;

  private static final int READ_REQUEST_CODE = 42;

    /** Acceleration required to detect a shake. In multiples of Earth's gravity. */
  private static final float SHAKE_THRESHOLD_GRAVITY = 2;

  /**
   * Advertise for 30 seconds before going back to discovering. If a client connects, we'll continue
   * to advertise indefinitely so others can still connect.
   */
  private static final long ADVERTISING_DURATION = 30000;

  /** How long to vibrate the phone when we change states. */
  private static final long VIBRATION_STRENGTH = 500;

  /** Length of state change animations. */
  private static final long ANIMATION_DURATION = 600;

  /**
   * This service id lets us find other nearby devices that are interested in the same thing. Our
   * sample does exactly one thing, so we hardcode the ID.
   */

  /**
   * The state of the app. As the app changes states, the UI will update and advertising/discovery
   * will start/stop.
   */
  private State mState = State.UNKNOWN;

  /** A random UID used as this device's endpoint name. */
  private String mName;

  /** Displays the previous state during animation transitions. */
  private TextView mPreviousStateView;

  /** Displays the current state. */
  private TextView mCurrentStateView;

  /** An animator that controls the animation from previous state to current state. */
  @Nullable private Animator mCurrentAnimator;

  /** A running log of debug messages. Only visible when DEBUG=true. */
  private TextView mDebugLogView;

  private Button mSendButton;

  private Button mRecieveButton;

  /** The File to be sent to nearby phones **/
  private File m_fileToSend;

  /** The SensorManager gives us access to sensors on the device. */
  private SensorManager mSensorManager;

  /** The accelerometer sensor allows us to detect device movement for shake-to-advertise. */
  private Sensor mAccelerometer;



  /** For recording audio as the user speaks. */


  /** The phone's original media volume. */
  private int mOriginalVolume;

  /**
   * A Handler that allows us to post back on to the UI thread. We use this to resume discovery
   * after an uneventful bout of advertising.
   */
  private final Handler mUiHandler = new Handler(Looper.getMainLooper());

  /** Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}. */
  private final Runnable mDiscoverRunnable =
      new Runnable() {
        @Override
        public void run() {
          setState(State.DISCOVERING);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


    mDebugLogView = (TextView) findViewById(R.id.debug_log);
    mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
    mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

    mRecieveButton = (Button) findViewById(R.id.receive_button);
    mSendButton = (Button) findViewById(R.id.send_button);

    mSendButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            performFileSearch();
        }
    });

    mRecieveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, ReceiverActivity.class);
        startActivity(intent);
      }
    });

    mName = Settings.Secure.getString(getContentResolver(), "device_name"); //generateRandomName();
    Constants.DEVICE_NAME = mName;

    ((TextView) findViewById(R.id.name)).setText(mName);
  }
  /**
   * Fires an intent to spin up the "file chooser" UI and select an image.
   */
  public void performFileSearch() {

    // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
    // browser.
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

    // Filter to only show results that can be "opened", such as a
    // file (as opposed to a list of contacts or timezones)
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    // Filter to show only images, using the image MIME data type.
    // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
    // To search for all documents available via installed storage providers,
    // it would be "*/*".
    intent.setType("*/*");
    startActivityForResult(intent, READ_REQUEST_CODE);
  }
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
      if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
          // The document selected by the user won't be returned in the intent.
          // Instead, a URI to that document will be contained in the return intent
          // provided to this method as a parameter.
          // Pull that URI using resultData.getData().
          Uri uri = null;
          if (resultData != null) {
              uri = resultData.getData();
              System.out.println(uri.getPath());
              Intent intent = new Intent(this, SenderActivity.class);
              intent.putExtra("FileUri", uri.toString());
              startActivity(intent);
          }

      }
  }



  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }



  @Override
  public void onBackPressed() {
    if (getState() == State.CONNECTED || getState() == State.ADVERTISING) {
      setState(State.DISCOVERING);
      return;
    }
    super.onBackPressed();
  }

  /**
   * We've connected to Nearby Connections. We can now start calling {@link #startDiscovering()} and
   * {@link #startAdvertising()}.
   */


  /**
   * The state has changed. I wonder what we'll be doing now.
   *
   * @param state The new state.
   */
  private void setState(State state) {

  }

  /** @return The current state. */
  private State getState() {
    return mState;
  }

  /**
   * State has changed.
   *
   * @param oldState The previous state we were in. Clean up anything related to this state.
   * @param newState The new state we're now in. Prepare the UI for this state.
   */
  private void onStateChanged(State oldState, State newState) {

  }

  /** Transitions from the old state to the new state with an animation implying moving forward. */
  @UiThread
  private void transitionForward(State oldState, final State newState) {
//    mPreviousStateView.setVisibility(View.VISIBLE);
//    mCurrentStateView.setVisibility(View.VISIBLE);
//
//    updateTextView(mPreviousStateView, oldState);
//    updateTextView(mCurrentStateView, newState);
//
//    mCurrentAnimator = createAnimator(false /* reverse */);
//    mCurrentAnimator.addListener(
//        new AnimatorListener() {
//          @Override
//          public void onAnimationEnd(Animator animator) {
//            updateTextView(mCurrentStateView, newState);
//          }
//        });
//    mCurrentAnimator.start();
  }

  /** Transitions from the old state to the new state with an animation implying moving backward. */
  @UiThread
  private void transitionBackward(State oldState, final State newState) {
//    mPreviousStateView.setVisibility(View.VISIBLE);
//    mCurrentStateView.setVisibility(View.VISIBLE);
//
//    updateTextView(mCurrentStateView, oldState);
//    updateTextView(mPreviousStateView, newState);
//
//    mCurrentAnimator = createAnimator(true /* reverse */);
//    mCurrentAnimator.addListener(
//        new AnimatorListener() {
//          @Override
//          public void onAnimationEnd(Animator animator) {
//            updateTextView(mCurrentStateView, newState);
//          }
//        });
//    mCurrentAnimator.start();
  }

  @NonNull
  private Animator createAnimator(boolean reverse) {
//    Animator animator;
//    if (Build.VERSION.SDK_INT >= 21) {
//      int cx = mCurrentStateView.getMeasuredWidth() / 2;
//      int cy = mCurrentStateView.getMeasuredHeight() / 2;
//      int initialRadius = 0;
//      int finalRadius = Math.max(mCurrentStateView.getWidth(), mCurrentStateView.getHeight());
//      if (reverse) {
//        int temp = initialRadius;
//        initialRadius = finalRadius;
//        finalRadius = temp;
//      }
//      animator =
//          ViewAnimationUtils.createCircularReveal(
//              mCurrentStateView, cx, cy, initialRadius, finalRadius);
//    } else {
//      float initialAlpha = 0f;
//      float finalAlpha = 1f;
//      if (reverse) {
//        float temp = initialAlpha;
//        initialAlpha = finalAlpha;
//        finalAlpha = temp;
//      }
//      mCurrentStateView.setAlpha(initialAlpha);
//      animator = ObjectAnimator.ofFloat(mCurrentStateView, "alpha", finalAlpha);
//    }
//    animator.addListener(
//        new AnimatorListener() {
//          @Override
//          public void onAnimationCancel(Animator animator) {
//            mPreviousStateView.setVisibility(View.GONE);
//            mCurrentStateView.setAlpha(1);
//          }
//
//          @Override
//          public void onAnimationEnd(Animator animator) {
//            mPreviousStateView.setVisibility(View.GONE);
//            mCurrentStateView.setAlpha(1);
//          }
//        });
//    animator.setDuration(ANIMATION_DURATION);
    return null;
  }

  /** Updates the {@link TextView} with the correct color/text for the given {@link State}. */
  @UiThread
  private void updateTextView(TextView textView, State state) {
    switch (state) {
      case DISCOVERING:
        textView.setBackgroundResource(R.color.state_discovering);
        textView.setText(R.string.status_discovering);
        break;
      case ADVERTISING:
        textView.setBackgroundResource(R.color.state_advertising);
        textView.setText(R.string.status_advertising);
        break;
      case CONNECTED:
        textView.setBackgroundResource(R.color.state_connected);
        textView.setText(R.string.status_connected);
        break;
      default:
        textView.setBackgroundResource(R.color.state_unknown);
        textView.setText(R.string.status_unknown);
        break;
    }
  }



  /** {@see Handler#post()} */
  protected void post(Runnable r) {
    mUiHandler.post(r);
  }

  /** {@see Handler#postDelayed(Runnable, long)} */
  protected void postDelayed(Runnable r, long duration) {
    mUiHandler.postDelayed(r, duration);
  }

  /** {@see Handler#removeCallbacks(Runnable)} */
  protected void removeCallbacks(Runnable r) {
    mUiHandler.removeCallbacks(r);
  }

  private void appendToLogs(CharSequence msg) {
    mDebugLogView.append("\n");
    mDebugLogView.append(DateFormat.format("hh:mm", System.currentTimeMillis()) + ": ");
    mDebugLogView.append(msg);
  }

  private static CharSequence toColor(String msg, int color) {
    SpannableString spannable = new SpannableString(msg);
    spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
    return spannable;
  }

  private static String generateRandomName() {
    String name = "";
    Random random = new Random();
    for (int i = 0; i < 5; i++) {
      name += random.nextInt(10);
    }
    return name;
  }

  @SuppressWarnings("unchecked")
  private static <T> T pickRandomElem(Collection<T> collection) {
    return (T) collection.toArray()[new Random().nextInt(collection.size())];
  }

  /**
   * Provides an implementation of Animator.AnimatorListener so that we only have to override the
   * method(s) we're interested in.
   */
  private abstract static class AnimatorListener implements Animator.AnimatorListener {
    @Override
    public void onAnimationStart(Animator animator) {}

    @Override
    public void onAnimationEnd(Animator animator) {}

    @Override
    public void onAnimationCancel(Animator animator) {}

    @Override
    public void onAnimationRepeat(Animator animator) {}
  }

  /** States that the UI goes through. */
  public enum State {
    UNKNOWN,
    DISCOVERING,
    ADVERTISING,
    CONNECTED
  }
}
