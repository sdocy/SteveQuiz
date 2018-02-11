package com.example.android.stevequiz;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer = null;



    int numQuestions = 10;


    // if any new per-game state is added, be sure to reset it in resetQuiz()
    int userAnswers[] = {0,0,0,0,0,0,0,0,0,0};
    boolean haveTheyAnswered[] = {false,false,false,false,false,false,false,false,false,false};
    int numCorrect = 0;         // number of question answered correctly
    LinearLayout currentLayout = null;          // what question layout is currently expanded
    LinearLayout currentAnswer = null;

    String playerName;          // player name, explicitly gotten from EditText

    // leaderboard
    int numSpots = 5;
    int scores[] = {0,0,0,0,0};
    TextView[] LBText = new TextView[numSpots];
    String names[] = {"","","","",""};
    // for flashing new LB score
    int newLBscore = -1;
    String LBColors[] = { "#3F51B5", "#3949AB", "#303F9F", "#283593", "#1A237E"};
    int colorIndex = 0;

    // expand/collapse a layout
    LinearLayout.LayoutParams closedParams =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
    LinearLayout.LayoutParams openParams =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    // stop spawning runnables for text flashing when we leave the results page
    boolean stopRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title);
        this.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    //
    // Called when user clicks start button on title screen.  Makes
    // sure the user has entered their name, if so, load the main content
    // and start playing Jeopardy song.
    //
    @SuppressWarnings("unused")
    void beginGame(View view) {
        EditText nameInput = findViewById(R.id.name_input);

        playerName = nameInput.getText().toString();
        if (playerName.matches("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "Please enter your name -->",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            return;
        }

        setContentView(R.layout.activity_main);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.jeopardy);
        mediaPlayer.start();
    }

    //
    // Called when user clicks Try Again button from the results scene.
    //  Resets game state, stops the text color runnable and loads the title scene.
    //
    @SuppressWarnings("unused")
    void backToTitle(View view) {
        stopRunnable = true;
        resetQuiz();
        setContentView(R.layout.title);
    }

    // Reset game state for a new game
    void resetQuiz() {
        currentLayout = null;
        currentAnswer = null;
        for (int i = 0; i < numQuestions; i++) {
            userAnswers[i] = 0;
            haveTheyAnswered[i] = false;
        }
        numCorrect = 0;
    }

    //
    // Called when a question button is hit.
    // - dynamically finds the LinearLayout sibling, for this to work a question
    //         button can only have a single sibling of type LinearLayout
    // - see if the layout for this question is already expanded
    //   - if so, collapse it
    // - else
    //   - see if there is a layout for another question expanded
    //      - if so, collapse it
    //   - expand the layout for this question
    //
    public void openQuestion(View view) {
        LinearLayout layout = (LinearLayout) view.getParent();

        // find sibling LinearLayout
        // requires only a single sibling of type LinearLayout
        LinearLayout answer = null;
        ViewGroup vg = (ViewGroup) view.getParent();
        for (int itemPos = 0; itemPos < vg.getChildCount(); itemPos++) {
            View v = vg.getChildAt(itemPos);
            if (v instanceof LinearLayout) {
                answer = (LinearLayout) v; //Found it!
                break;
            }
        }

        if (answer == null) {
            Log.v("ERROR", "Null answer layout openQuestion()");
            return;
        }

        if (currentLayout == layout) {
            // this view is expanded, collapse it
            answer.setLayoutParams(closedParams);
            layout.setBackgroundColor(0xff076379);
            currentLayout = null;
            currentAnswer = null;
        } else {
            if (currentLayout != null) {
                // a different view is expanded, collapse it
                currentAnswer.setLayoutParams(closedParams);
                currentLayout.setBackgroundColor(0xff076379);
            }

            // expand the view for this question
            answer.setLayoutParams(openParams);
            layout.setBackgroundColor(0xff7fc2e4);
            currentLayout = layout;
            currentAnswer = answer;
        }
    }

    // update the number of correct answers
    void scoreAnswers() {
        for (int i = 0; i < numQuestions; i++) {
            if (i == 7) {
                if (userAnswers[i] == 4)
                    numCorrect++;
            } else {
                if (userAnswers[i]  == 1)
                    numCorrect++;
            }
        }
    }

    // see if the user has answered all questions
    boolean answeredAllQuestions() {
        for (int i = 0; i < numQuestions; i++)
            if (!haveTheyAnswered[i])
                return false;

        return true;
    }

    // display a message with number correct and some witty flavor text to go with it
    void displayResults() {
        TextView summaryTextView = findViewById(R.id.summaryText);
        TextView flavorTextView = findViewById(R.id.flavorText);
        String tmpStr;

        tmpStr = "You got\n" + " " + numCorrect + "\nout of " + numQuestions + "\n";
        summaryTextView.setText(tmpStr);

        switch (numCorrect) {
            case 0:
            case 1:
            case 2:
            case 3: tmpStr = "Steve Who?\n";
                    break;
            case 4:
            case 5:
            case 6: tmpStr = "Better go study\n";
                    break;
            case 7:
            case 8: tmpStr = "You are worthy\n";
                    break;
            case 9: tmpStr = "You Grok Steve\n";

                    break;
            case 10: tmpStr = "I.....AM....STEVE\n";
                    break;
        }
        flavorTextView.setText(tmpStr);
    }

    //
    // if new score is greater than the smallest score on the leaderboard,
    // add this score and update the leaderboard TextViews
    //
    void displayLeaderBoard() {
        // get text view references
        //TextView[] LBText = new TextView[numSpots];
        LBText[0] = findViewById(R.id.LB1);
        LBText[1] = findViewById(R.id.LB2);
        LBText[2] = findViewById(R.id.LB3);
        LBText[3] = findViewById(R.id.LB4);
        LBText[4] = findViewById(R.id.LB5);

        // insert new score
        newLBscore = -1;
        if (numCorrect > scores[numSpots - 1]) {
            scores[numSpots - 1] = numCorrect;
            names[numSpots - 1] = playerName;
            newLBscore = numSpots - 1;
            for (int i = numSpots - 2; i >= 0; i--) {
                if (scores[i + 1] > scores[i]) {
                    int tmpScore = scores[i];
                    scores[i] = scores[i + 1];
                    scores[i + 1] = tmpScore;

                    String tmpName = names[i];
                    names[i] = names[i + 1];
                    names[i + 1] = tmpName;
                    newLBscore = i;
                } else {
                    break;
                }
            }
        }

        // update text views
        for (int  i = 0; i < numSpots; i++) {
            if (scores[i] == 0)
                break;

            String tmpStr = names[i] + "   " + scores[i];
            LBText[i].setText(tmpStr);
        }

        messWithColors();
    }

    // If the user has answered all questions, calculate their score and load the results scene.
    public void showResults(View view) {

        if (!answeredAllQuestions()) {
            Toast toast = Toast.makeText(getApplicationContext(), "Please answer all questions",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            return;
        }

        scoreAnswers();
        setContentView(R.layout.results);
        displayResults();
        displayLeaderBoard();
    }

    // handler to flash a new score added to the LB
    Handler handler = new Handler();
    final Runnable r = new Runnable() {
        public void run() {
            if (newLBscore == -1)
                return;
            LBText[newLBscore].setTextColor(Color.parseColor(LBColors[colorIndex]));
            // get next color for next run
            colorIndex = (colorIndex +1) % numSpots;
            if (!stopRunnable)
                handler.postDelayed(this, 100);
        }
    };
    void messWithColors() {
        stopRunnable = false;
        handler.postDelayed(r, 300);
    }



    //
    // The remaining methods are called when specific question responses are chosen (radio buttons,
    // check boxes).  They :
    // - get the view tag
    //   - correct answers have a tag of 1
    //   - incorrect answers have a tag of 0 for radio buttons and -1 for checkboxes
    //   - incorrect answers with easter eggs have a tag of -2
    // - perform easter egg if appropriate
    // - record user's answer to this question and mark the question as answered
    // - darken this button so the user knows they have answered this question
    //
    public void answer1(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "You shall not pass!",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = 0;
        }

        userAnswers[1] = answer;
        haveTheyAnswered[1] = true;

        Button btn = findViewById(R.id.button1);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer2(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "Wax on, wax off",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = 0;
        }

        userAnswers[2] = answer;
        haveTheyAnswered[2] = true;

        Button btn = findViewById(R.id.button2);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer3(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.nightbegins);
            mediaPlayer.start();

            answer = 0;
        }

        userAnswers[3] = answer;
        haveTheyAnswered[3] = true;

        Button btn = findViewById(R.id.button3);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer4(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "Not a pizza topping",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = 0;
        }

        userAnswers[4] = answer;
        haveTheyAnswered[4] = true;

        Button btn = findViewById(R.id.button4);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer5(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "Yeah, she wishes",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = 0;
        }

        userAnswers[5] = answer;
        haveTheyAnswered[5] = true;

        Button btn = findViewById(R.id.button5);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer6(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "Yeah, but he still looks gooooood",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = 0;
        }

        userAnswers[6] = answer;
        haveTheyAnswered[6] = true;

        Button btn = findViewById(R.id.button6);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer7(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "Why so serious?",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = -1;
        }

        if (((CheckBox)view).isChecked()) {
            userAnswers[7] += answer;
        } else {
            userAnswers[7] -= answer;
        }

        haveTheyAnswered[7] = true;

        Button btn = findViewById(R.id.button7);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer8(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        userAnswers[8] = answer;
        haveTheyAnswered[8] = true;

        Button btn = findViewById(R.id.button8);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer9(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            Toast toast = Toast.makeText(getApplicationContext(), "ACK",
                    Toast.LENGTH_SHORT);
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.CYAN);
            toast.show();
            answer = 0;
        }

        userAnswers[9] = answer;
        haveTheyAnswered[9] = true;

        Button btn = findViewById(R.id.button9);
        btn.setBackgroundResource(R.drawable.med_bg);
    }

    public void answer0(View view) {
        ImageView answer0Pic = findViewById(R.id.scmarsPic);
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            answer0Pic.setLayoutParams(openParams);
            answer = 0;
        } else {
            answer0Pic.setLayoutParams(closedParams);
        }

        userAnswers[0] = answer;
        haveTheyAnswered[0] = true;

        Button btn = findViewById(R.id.button0);
        btn.setBackgroundResource(R.drawable.med_bg);
    }
}
