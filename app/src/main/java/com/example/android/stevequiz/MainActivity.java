package com.example.android.stevequiz;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

// "Santa Claus Conquers the Martians" photo courtesy of http://assets.nydailynews.com/polopoly_fs/1.1133847.1344638915!/img/httpImage/image.jpg_gen/derivatives/gallery_320/santa-claus-conquers-martians.jpg
// "Jeopardy" music courtesy of http://www.orangefreesounds.com/jeopardy-theme-song/
// "The Night Begins To Shine" music courtesy of https://1music-online.info/?q=The-Night-Begins-to-Shine---BER&youtube=on

public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer = null;

    // dynamic question background colors
    int lightBlue;          // question background color when expanded
    int blueGreen;          // question background color when collapsed

    int numQuestions = 10;              // how many questions are there?

    // if any new per-game state is added, be sure to reset it in resetQuiz()
    int userAnswers[] = {0,0,0,0,0,0,0,0,0,0};       // store a key indicating correct/incorrect answer
    boolean haveTheyAnswered[] = {false,false,false,false,false,false,false,false,false,false};
    int numCorrect = 0;         // number of question answered correctly
    LinearLayout currentLayout = null;          // what question layout is currently expanded
    LinearLayout currentAnswer = null;

    String playerName;          // player name, explicitly gotten from EditText

    // leaderboard
    int numSpots = 5;                               // number of scores on leaderboard
    int scores[] = {0,0,0,0,0};                     // high scores
    TextView[] LBText = new TextView[numSpots];     // views for displaying high scores
    String names[] = {"","","","",""};              // high scores player names

    // for flashing new LB score
    int newLBscore = -1;                        // stores the position in the LB a new score gets
    String LBColors[] = { "#3F51B5", "#3949AB", "#303F9F", "#283593", "#1A237E"};
    int colorIndex = 0;                         // used to cycle through LB colors for new score

    // expand/collapse a question layout
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

    // Called when user clicks start button on title activity.  Makes
    // sure the user has entered their name, if so, load the main content
    // and start playing Jeopardy song.
    @SuppressWarnings("unused")
    public void beginGame(View view) {
        EditText nameInput = findViewById(R.id.name_input);

        playerName = nameInput.getText().toString();
        if (playerName.equalsIgnoreCase("")) {
            showToast(getString(R.string.enter_name));
            return;
        }

        // load question activity
        setContentView(R.layout.activity_main);

        // simplest method I found for changing music for mediaPlayer is to destroy it and recreate it
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.jeopardy);
        mediaPlayer.start();

        lightBlue = ContextCompat.getColor(getApplicationContext(), R.color.lightBlue);
        blueGreen = ContextCompat.getColor(getApplicationContext(), R.color.blueGreen);
    }

    // Called when user clicks Try Again button from the results activity.
    //  Resets game state, stops the text color runnable and loads the title activity.
    @SuppressWarnings("unused")
    public void backToTitle(View view) {
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

    // Called when user clicks Submit Answers.
    // If the user has answered all questions, calculate their score and load the results activity.
    public void showResults(View view) {

        if (!answeredAllQuestions()) {
            showToast(getString(R.string.all_questions));
            return;
        }

        scoreAnswers();
        setContentView(R.layout.results);
        displayResults();
        displayLeaderBoard();
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

    // has the user answered all questions?
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

        tmpStr = getString(R.string.you_got) + "\n" + numCorrect + "\n" + getString(R.string.out_of) + " " + numQuestions + "\n";
        summaryTextView.setText(tmpStr);

        switch (numCorrect) {
            case 0:
            case 1:
            case 2:
            case 3: tmpStr = getString(R.string.steve_who);
                    break;
            case 4:
            case 5:
            case 6: tmpStr = getString(R.string.go_study);
                    break;
            case 7:
            case 8: tmpStr = getString(R.string.worthy);
                    break;
            case 9: tmpStr = getString(R.string.grok);

                    break;
            case 10: tmpStr = getString(R.string.am_steve);
                    break;
        }
        flavorTextView.setText(tmpStr);
    }

    // if new score is greater than the smallest score on the leaderboard,
    // add this score and update the leaderboard TextViews
    void displayLeaderBoard() {
        // get text view references for each spot on the LB
        LBText[0] = findViewById(R.id.LB1);
        LBText[1] = findViewById(R.id.LB2);
        LBText[2] = findViewById(R.id.LB3);
        LBText[3] = findViewById(R.id.LB4);
        LBText[4] = findViewById(R.id.LB5);

        // insert new score
        newLBscore = -1;
        if (numCorrect > scores[numSpots - 1]) {
            // score made it onto the LB, insert it in the bottom slot
            scores[numSpots - 1] = numCorrect;
            names[numSpots - 1] = playerName;

            newLBscore = numSpots - 1;
            for (int i = numSpots - 2; i >= 0; i--) {
                // migrate new score up to its proper spot on the LB
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

        // display leader board
        for (int  i = 0; i < numSpots; i++) {
            if (scores[i] == 0)
                break;

            String tmpStr = names[i] + " " + scores[i];
            LBText[i].setText(tmpStr);
        }

        // cause new score to flash
        if (newLBscore != -1)
            messWithColors();
    }

    // Handler to flash a new score added to the LB.
    // It changes the text color to the next text color in LBcolors, and then
    // sets up another execution of itself in 0.1 seconds.
    // stopRunnable is set to true to stop the recursion when the user clicks
    // Try Again to go back to the title activity.
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

    // called from displayLeaderBoard() to trigger flashing for new score on LB
    void messWithColors() {
        stopRunnable = false;
        handler.postDelayed(r, 300);
    }

    // Called when a question button is hit.
    // - dynamically finds the LinearLayout sibling, for this to work a question
    //         button can only have a single sibling of type LinearLayout
    // - see if the layout for this question is already expanded
    //   - if so, collapse it
    // - else
    //   - see if there is a layout for another question expanded
    //      - if so, collapse it
    //   - expand the layout for this question
    @SuppressWarnings("unused")
    public void openQuestion(View view) {
        LinearLayout layout = (LinearLayout) view.getParent();

        // find sibling LinearLayout
        // requires there is only a single sibling of type LinearLayout
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

        // use setLayoutParams() to set the width and height ofviews when a question is chosen
        if (currentLayout == layout) {
            // this view is already expanded, collapse it
            answer.setLayoutParams(closedParams);
            layout.setBackgroundColor(blueGreen);
            currentLayout = null;
            currentAnswer = null;
        } else {
            if (currentLayout != null) {
                // a different view is expanded, collapse it
                currentAnswer.setLayoutParams(closedParams);
                currentLayout.setBackgroundColor(blueGreen);
            }

            // expand the view for this question
            answer.setLayoutParams(openParams);
            layout.setBackgroundColor(lightBlue);

            // remember which layout and answer are expanded
            currentLayout = layout;
            currentAnswer = answer;
        }
    }

    // pop-up a toast
    void showToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        TextView toastMessage = toast.getView().findViewById(android.R.id.message);
        toastMessage.setTextColor(Color.CYAN);
        toast.show();
    }

    // The remaining methods are called when specific question responses are chosen (radio buttons,
    // check boxes).  They :
    // - get the view tag
    //   - correct answers have a tag of 1
    //   - incorrect answers have a tag of 0 for radio buttons and -1 for checkboxes
    //   - incorrect answers with easter eggs have a tag of -2
    // - perform easter egg if appropriate, and fix tag value to that equal an incorrect answer
    // - record user's answer to this question and mark the question as answered
    // - darken this button so the user knows they have answered this question

    public void answer1(View view) {
        int answer = Integer.parseInt(view.getTag().toString());

        if (answer == -2) {
            showToast(getString(R.string.not_pass));
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
            showToast(getString(R.string.wax_on));
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
            showToast(getString(R.string.not_topping));
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
            showToast(getString(R.string.she_wishes));
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
            showToast(getString(R.string.looks_good));
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
            showToast(getString(R.string.serious));
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
            showToast(getString(R.string.ack));
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
