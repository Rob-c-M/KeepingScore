package com.robmapps.keepingscore;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.IntentFilter;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ObjectAnimator;
import android.animation.Animator;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robmapps.keepingscore.database.AppDatabase;
import com.robmapps.keepingscore.database.entities.GameStats;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.util.Map;

public class Frag_Gameplay extends Fragment {
    public TextView tvScore1, tvScore2, tvTimeRem, tvGameTitle, tvQuarterNum, tvTeam1;
    public EditText etTeam2;
    public int iScore1, iScore2, iGS1, iGA1, iGS2, iGA2, iGS1M, iGA1M, iGS2M, iGA2M, iNumPers, iPerDuration, iPerNum, iLength;
    public long TimeMultiplier;
    public String sScore1, timeFormatted, sTeam1, sTeam2, StatsFileName, sGSPlayer, sGAPlayer, sCurrMode;
    public Boolean bTimerRunning = false, bDebugMode = false;
    public Button btnShowStats, btnStartGame, btnBestOnCourt, btnUndo, btnGameMode, btnReset, btnTeamList;
    public Button btnGS1, btnGA1, btnGS1M, btnGA1M, btnGS2, btnGA2, btnGS2M, btnGA2M;
    public SharedPreferences spSavedValues;
    public CountDownTimer cdEndofPeriodTimer;
    public StringBuilder sbExportStats; // sAllActions,
    private SharedViewModel viewModel;
    private ImageView ivCentrePassCircle;
    private ObjectAnimator animatorY; // To control the animation
    private boolean movingToEndLocation = true; // To track animation direction
    private float startX, startY, endX, endY;   // Coordinates for movement
    // Below is vibration patterns
    long[] patternGoal = {0, 800, 400};
    long[] patternMiss = {0, 100, 100, 100, 100};
    long[] patternEndGame = {1, 100, 1000, 300, 200, 100, 500, 200, 100};
    long[] patternEndPeriod = {1, 100, 1000, 300, 200, 100, 500, 200, 100};
    private float target1CenterX, target1CenterY;
    private float target2CenterX, target2CenterY;
    private boolean coordinatesInitialized = false;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gameplay, container, false);
        LinearLayout LinearLayout = view.findViewById(R.id.gameplay_root_layout);
        setupUI(LinearLayout);

        //sAllActions = new StringBuilder(0);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        tvScore1 = view.findViewById(R.id.Team1Score);
        tvScore2 = view.findViewById(R.id.Team2Score);
        tvGameTitle = view.findViewById(R.id.GameTitle);
        tvTeam1 = view.findViewById(R.id.T1Name);
        tvTeam1.setEnabled(true);
        etTeam2 = view.findViewById(R.id.T2Name); // Initialize EditText for Team 2
        btnUndo = view.findViewById(R.id.UndoButton);
        btnReset = view.findViewById(R.id.ResetGame);
        btnGS1 = view.findViewById(R.id.GS1);
        btnGA1 = view.findViewById(R.id.GA1);
        btnGS1M = view.findViewById(R.id.GS1Miss);
        btnGA1M = view.findViewById(R.id.GA1Miss);
        btnGS2 = view.findViewById(R.id.GS2);
        btnGA2 = view.findViewById(R.id.GA2);
        btnGS2M = view.findViewById(R.id.GS2Miss);
        btnGA2M = view.findViewById(R.id.GA2Miss);
        //btnShowStats = view.findViewById(R.id.Statistics);
        tvTimeRem = view.findViewById(R.id.TimeRem);
        tvQuarterNum = view.findViewById(R.id.QuarterNum);
        btnGameMode = view.findViewById(R.id.GameMode);
        ivCentrePassCircle = view.findViewById(R.id.centrePassCircle);
        if (sGSPlayer != null) {
            btnGS1.setText("GS" + "\n" + sGSPlayer);
            btnGA1.setText("GA" + "\n" + sGAPlayer);
        }


        sCurrMode = viewModel.getGameMode().getValue().toString();
        //sCurrMode="15m,4Q"; // Default game mode
        Log.d("GameVariables", "Game Mode = " + sCurrMode); // Log when observer is triggered
        if (sCurrMode.length() > 8) {
            sCurrMode = "15m,4Q"; // Default game mode
            btnGameMode.setText(sCurrMode);
        } else {
            btnGameMode.setText(sCurrMode);
        }
        if (viewModel.getGameInProgress().getValue() == true) {
            btnGS1.setEnabled(true);
            btnGA1.setEnabled(true);
            btnGS1M.setEnabled(true);
            btnGA1M.setEnabled(true);
            btnGS2.setEnabled(true);
            btnGA2.setEnabled(true);
            btnGS2M.setEnabled(true);
            btnGA2M.setEnabled(true);
            btnGameMode.setEnabled(false);
            btnStartGame.setEnabled(false);
            bTimerRunning = true;
        } else {
            btnGS1.setEnabled(false);
            btnGA1.setEnabled(false);
            btnGS1M.setEnabled(false);
            btnGA1M.setEnabled(false);
            btnGS2.setEnabled(false);
            btnGA2.setEnabled(false);
            btnGS2M.setEnabled(false);
            btnGA2M.setEnabled(false);
            btnGameMode.setEnabled(true);
            bTimerRunning = false;
        }

        // Observe Active Team Name
        viewModel.getActiveTeam().observe(getViewLifecycleOwner(), activeTeam -> {
            Log.d("GameplayActiveTeamObs", "Observer triggered."); // Log when observer is triggered

            if (activeTeam != null) {
                Log.d("GameplayActiveTeamObs", "Active team is NOT null.");
                if (activeTeam.getTeamName() != null) {
                    tvTeam1.setText(activeTeam.getTeamName());
                    tvTeam1.requestLayout(); // Request layout pass
                    tvTeam1.invalidate();    // Request redraw pass
                    // --- Iterate through players to find GS and GA ---
                    List<Player> players = activeTeam.getPlayers();
                    sGSPlayer = null; // Initialize to null before searching
                    sGAPlayer = null; // Initialize to null before searching

                    if (players != null) {
                        for (Player player : players) {
                            if (player.getPosition() != null && player.getPosition().equals("GS")) {
                                sGSPlayer = player.getName();
                            } else if (player.getPosition() != null && player.getPosition().equals("GA")) {
                                sGAPlayer = player.getName();
                            }
                            // You can add checks for other positions here if needed
                        }
                    }
                    updateGameTitle();
                } else {
                    Log.d("GameplayActiveTeamObs", "Active team name IS null.");
                    tvTeam1.setText(""); // Clear if name is null
                    sGSPlayer = null;
                    sGAPlayer = null;
                    updateGameTitle();
                }
            } else {
                Log.d("GameplayActiveTeamObs", "Active team IS null.");
                sTeam1 = "";
                tvTeam1.setText(""); // Assuming you want to clear the text if no active team
                sGSPlayer = null;
                sGAPlayer = null;
                updateGameTitle();
            }
        });

        // Observe Team 2 name from the ViewModel's SavedStateHandle
        viewModel.getTeam2Name().observe(getViewLifecycleOwner(), team2Name -> {
            Log.d("GameplayTeam2Observer", "Team 2 name observed: " + team2Name);
            // Only update the EditText if the current text is different
            // This prevents an infinite loop caused by the TextWatcher
            if (!etTeam2.getText().toString().equals(team2Name)) {
                etTeam2.setText(team2Name); // Set the EditText text
            }
        });

        // Observe LiveData
        viewModel.getTeam1Score().observe(getViewLifecycleOwner(), score -> {
            tvScore1.setText(String.valueOf(score));
        });
        viewModel.getTeam2Score().observe(getViewLifecycleOwner(), score -> {
            tvScore2.setText(String.valueOf(score));
        });


        // Set listeners for Team 1 buttons
        btnGS1.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore1, "GS1", sGSPlayer, true);
        });
        btnGA1.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore1, "GA1", sGAPlayer, true);
        });
        btnGS1M.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore1, "GS1", sGSPlayer, false);
        });
        btnGA1M.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore1, "GA1", sGAPlayer, false);
        });
        // Set listeners for Team 2 buttons
        btnGS2.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore2, "GS2", "Other", true);
        });
        btnGA2.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore2, "GA2", "Other", true);
        });
        btnGS2M.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore2, "GS2", "Other", false);
        });
        btnGA2M.setOnClickListener(v -> {
            incrementScore(viewModel, tvScore2, "GA2", "Other", false);
        });
        //btnTeamList = view.findViewById(R.id.btnTeamsLists);

        view.findViewById(R.id.Team1Score).setOnClickListener(v -> {
            viewModel.swapCentrePass();
            startImageAnimation();
        });
        view.findViewById(R.id.Team2Score).setOnClickListener(v -> {
            viewModel.swapCentrePass();
            startImageAnimation();
        });
        btnStartGame = view.findViewById(R.id.btnStartGame);

        btnUndo.setOnClickListener(v -> {
            undoLastAction(viewModel);
        });
        sCurrMode = btnGameMode.getText().toString(); // Default mode
        btnGameMode.setOnClickListener(v -> {
            GameMode(view);
        });
        btnStartGame.setOnClickListener(v -> startGameTimer());
        btnGameMode.setOnLongClickListener(v -> GameModeDebug());
        viewModel.setGameMode(sCurrMode);
        btnReset.setOnClickListener(v -> resetGame());

        // Observe Centre-Pass State and Colors
        viewModel.getCurrentCentrePass().observe(getViewLifecycleOwner(), centrePass -> {
            Log.d("CentrePass", "Current Centre-Pass: " + centrePass);
        });
        viewModel.getTeam1ScoreColor().observe(getViewLifecycleOwner(), color -> {
            tvScore1.setTextColor(color);
        });
        viewModel.getTeam2ScoreColor().observe(getViewLifecycleOwner(), color -> {
            tvScore2.setTextColor(color);
        });

        // Register BroadcastReceiver for Timer Updates
        Context context = getContext();
        if (context != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("GAME_TIMER_UPDATE");
            filter.addAction("END_OF_PERIOD_ACTION");
            context.registerReceiver(timerReceiver, filter);
        } else {
            Log.e("Frag_Gameplay", "Context is null, BroadcastReceiver not registered");
        }
        etTeam2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed here
            }

            @Override
            public void afterTextChanged(Editable s) {
                // When Team 2 name is changed, update the game title
                updateGameTitle();
                viewModel.saveTeam2Name(s.toString());
            }
        });
        updateGameTitle();
        return view;
    }

    private Void resetGame() {
        /*Routine to reset the game to zeros*/
        /*Done Make a confirmation requirement*/
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(requireContext());
        adBuilder.setMessage("Confirm Clear current game?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                iScore1 = 0;
                iScore2 = 0;
                viewModel.resetGame();
                iGS1 = 0;
                iGA1 = 0;
                iGS1M = 0;
                iGA1M = 0;
                iGS2 = 0;
                iGA2 = 0;
                iGS2M = 0;
                iGA2M = 0;
                iPerNum = 1;
                btnGS1.setEnabled(false);
                btnGA1.setEnabled(false);
                btnGS1M.setEnabled(false);
                btnGA1M.setEnabled(false);
                btnGS2.setEnabled(false);
                btnGA2.setEnabled(false);
                btnGS2M.setEnabled(false);
                btnGA2M.setEnabled(false);

                if (viewModel.getGameInProgress().getValue() == true) {
                    viewModel.setGameInProgress(false);
                    Intent stopIntent = new Intent(requireContext(), TimerService.class);
                    requireContext().stopService(stopIntent); // Stop the service
                    bTimerRunning = false; // Update the flag
                    Toast.makeText(requireContext(), "Game Timer stopped.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Game Timer isn't running", Toast.LENGTH_SHORT).show();
                }
                if (cdEndofPeriodTimer == null) {
                    //Timer doesn't exist, or isn't running
                    // Toast.makeText(requireContext(), "ExtraTime Timer isn't running", Toast.LENGTH_SHORT).show();
                } else {
                    cdEndofPeriodTimer.cancel();
                }
                btnStartGame.setEnabled(true);
                tvTimeRem.setTextColor(Color.rgb(0, 0, 0));
                tvScore1.setText("" + iScore1);
                tvScore2.setText("" + iScore2);

                tvTimeRem.setText("Timer");
                tvQuarterNum.setText("Quarter");
                btnGameMode.setEnabled(true);
                onPause();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //
            }
        });
        AlertDialog mDialog = adBuilder.create();
        mDialog.show();
        return null;
    }

    private void incrementScore(SharedViewModel viewModel, TextView scoreView, String playerPosition, String playerName, boolean isSuccessful) {
        if (sGSPlayer != null) {
            btnGS1.setText("GS" + "\n" + sGSPlayer);
            btnGA1.setText("GA" + "\n" + sGAPlayer);
        }
        if (isSuccessful) {
            if (scoreView == tvScore1) {
                viewModel.updateTeam1Score(1); // Increment score for Team 1
            } else if (scoreView == tvScore2) {
                viewModel.updateTeam2Score(1); // Increment score for Team 2
            }
            startImageAnimation();
            viewModel.swapCentrePass();
        }

        if (playerPosition == "GS1") {
            playerName = sGSPlayer;
        } else {
            if (playerPosition == "GA1") {
                playerName = sGAPlayer;
            } else {
                playerName = "Other Team";
            }
        }
        viewModel.recordAttempt(playerName, playerPosition, isSuccessful, timeFormatted);
    }

    private void undoLastAction(SharedViewModel viewModel) {
        List<ScoringAttempt> currentActions = viewModel.getAllActions().getValue();
        if (currentActions != null && !currentActions.isEmpty()) {
            ScoringAttempt lastAction = currentActions.remove(currentActions.size() - 1);

            // Use the new method in SharedViewModel to update LiveData
            viewModel.updateAllActions(currentActions);

            // Adjust score if it was a successful goal
            if (lastAction.isSuccessful()) {
                if (lastAction.getPlayerPosition().startsWith("GS1") || lastAction.getPlayerPosition().startsWith("GA1")) {
                    viewModel.updateTeam1Score(-1); // Decrement score for Team 1
                    viewModel.swapCentrePass();
                    startImageAnimation();
                } else if (lastAction.getPlayerPosition().startsWith("GS2") || lastAction.getPlayerPosition().startsWith("GA2")) {
                    viewModel.updateTeam2Score(-1); // Decrement score for Team 2
                    viewModel.swapCentrePass();
                    startImageAnimation();
                }
            }
        }
    }

    public void GameMode(View view) {
        bDebugMode = false;
        if (sCurrMode == null) {
            sCurrMode = "15m,4Q"; // Fallback to a default value if null
        }
        switch (sCurrMode) {
            case "GameMode":
            case "01m,2H":
                sCurrMode = "10m,4Q";
                iPerDuration = 10;
                iNumPers = 4;
                btnGameMode.setText(sCurrMode);
                break;
            case "10m,4Q":
                sCurrMode = "12m,4Q";
                iPerDuration = 12;
                iNumPers = 4;
                btnGameMode.setText(sCurrMode);
                break;
            case "12m,4Q":
                sCurrMode = "15m,4Q";
                iPerDuration = 15;
                iNumPers = 4;
                btnGameMode.setText(sCurrMode);
                break;
            case "15m,4Q":
                sCurrMode = "10m,2H";
                iPerDuration = 10;
                iNumPers = 2;
                btnGameMode.setText(sCurrMode);
                break;
            case "10m,2H":
                sCurrMode = "12m,2H";
                iPerDuration = 10;
                iNumPers = 2;
                btnGameMode.setText(sCurrMode);
                break;
            case "12m,2H":
                sCurrMode = "15m,2H";
                iPerDuration = 10;
                iNumPers = 4;
                btnGameMode.setText(sCurrMode);
                break;
            case "15m,2H":
                sCurrMode = "10m,4Q";
                iPerDuration = 10;
                iNumPers = 4;
                btnGameMode.setText(sCurrMode);
                break;
        }
        viewModel.setGameMode(sCurrMode);
        Log.d("GameVariables", "Game Mode (local) = " + sCurrMode); // Log when observer is triggered
        Log.d("GameVariables", "Game Mode (SVM) = " + viewModel.getGameMode().getValue()); // Log when observer is triggered
    }

    public boolean GameModeDebug() {
        sCurrMode = "01m,2H";
        iPerDuration = 1;
        iNumPers = 2;
        btnGameMode.setText(sCurrMode);
        bDebugMode = true;
        viewModel.setGameMode(sCurrMode);
        //Toast.makeText(requireContext(),"Debug mode",Toast.LENGTH_SHORT).show();
        Log.d("GameVariables", "Game Mode = " + sCurrMode); // Log when observer is triggered
        return true;
    }

    private void startGameTimer() {
        if (viewModel.getGameInProgress().getValue() == true) {
            Toast.makeText(requireContext(), "Game Timer is already running!", Toast.LENGTH_SHORT).show();
            btnStartGame.setEnabled(false);
            return;
        }

        btnGS1.setEnabled(true);
        btnGA1.setEnabled(true);
        btnGS1M.setEnabled(true);
        btnGA1M.setEnabled(true);
        btnGS2.setEnabled(true);
        btnGA2.setEnabled(true);
        btnGS2M.setEnabled(true);
        btnGA2M.setEnabled(true);
        btnGS1.setText("GS" + "\n" + sGSPlayer);
        btnGA1.setText("GA" + "\n" + sGAPlayer);
        sCurrMode = btnGameMode.getText().toString();

        if (iPerNum < 1) {
            iPerNum++;
        }
        viewModel.setGameInProgress(true);
        viewModel.setCurrentPeriod(iPerNum);

        parseGameMode(); // Ensure period duration and number of periods are set
        // Start the timer service
        Intent intent = new Intent(requireContext(), TimerService.class);
        intent.putExtra("PERIOD_DURATION", iPerDuration); // Pass the period duration
        intent.putExtra("TOTAL_PERIODS", iNumPers);       // Pass the total number of periods
        intent.putExtra("CURRENT_PERIOD", iPerNum);            // Start with the first period
        requireContext().startService(intent);

        btnStartGame.setEnabled(false);
        bTimerRunning = true; // Set the timer state to running
        Toast.makeText(requireContext(), "Game Timer Started!", Toast.LENGTH_SHORT).show();
        btnGameMode.setEnabled(false);

        int[] tvScore1ScreenCoordinates = new int[2];
        int[] tvScore2ScreenCoordinates = new int[2];
        tvScore1.getLocationOnScreen(tvScore1ScreenCoordinates);
        tvScore2.getLocationOnScreen(tvScore2ScreenCoordinates);

        startX = tvScore1ScreenCoordinates[0] + (tvScore1.getWidth() / 2f) - (ivCentrePassCircle.getWidth() / 2f);
        startY = tvScore1ScreenCoordinates[1] - (tvScore1.getHeight() / 2f) - (ivCentrePassCircle.getHeight() / 3f);
        endX = tvScore2ScreenCoordinates[0] + (tvScore2.getWidth() / 2f) - (ivCentrePassCircle.getWidth() / 2f);
        endY = tvScore2ScreenCoordinates[1] - (tvScore2.getHeight() / 2f) - (ivCentrePassCircle.getHeight() / 3f);
        ivCentrePassCircle.setX(startX);
        ivCentrePassCircle.setY(startY);
        ivCentrePassCircle.setVisibility(View.VISIBLE);
    }

    private void EndOfPeriodTimer() {
        tvTimeRem.setTextColor(Color.rgb(255, 150, 0));
        if (bDebugMode) {
            TimeMultiplier = 10000;
        } else {
            TimeMultiplier = 60000;
        }


        cdEndofPeriodTimer = new CountDownTimer(TimeMultiplier / 3, 1000) { /* x minutes countdown after normal period is ended, until buttons are disabled.*/
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = (millisUntilFinished / 1000 / 3600);
                long minutes = ((millisUntilFinished / 1000) % 3600) / 60;
                long seconds = (millisUntilFinished / 1000 % 60);
                timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

                tvTimeRem.setText("" + timeFormatted);
                viewModel.setGameInProgress(true);
                bTimerRunning = true;
            }

            @Override
            public void onFinish() {
                bTimerRunning = false;
                viewModel.setGameInProgress(false);
                btnGS1.setEnabled(false);
                btnGA1.setEnabled(false);
                btnGS1M.setEnabled(false);
                btnGA1M.setEnabled(false);
                btnGS2.setEnabled(false);
                btnGA2.setEnabled(false);
                btnGS2M.setEnabled(false);
                btnGA2M.setEnabled(false);

                if (iPerNum < iNumPers) {
                    if (iNumPers == 2) {
                        tvQuarterNum.setText("End of H: " + iPerNum);
                    } else {
                        tvQuarterNum.setText("End of Q: " + iPerNum);
                    }
                    viewModel.recordAttempt("\n-------========-------", "", false, timeFormatted);
                    btnStartGame.setEnabled(true);
                    iPerNum++;
                    MakeitShake(patternEndPeriod);
                } else {
                    tvQuarterNum.setText("Game Over");
                    btnStartGame.setEnabled(false);
                    MakeitShake(patternEndGame);

//TODO Auto change to Stats page after game finishes
/*
                    Frag_Stats fragStats = new Frag_Stats();
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainerView1,fragStats)
                            .addToBackStack(null) // Optional, allows back navigation to gameplay
                            .commit();
*/
                }
            }
        }.start();
    }

    private void parseGameMode() {
        if (sCurrMode != null && !sCurrMode.isEmpty()) {
            int duration = Integer.parseInt(sCurrMode.substring(0, 2)); // Extract duration (first two digits)
            iPerDuration = duration; // Set period duration in minutes

            if (sCurrMode.contains("2H")) {
                iNumPers = 2; // 2 Halves
            } else if (sCurrMode.contains("4Q")) {
                iNumPers = 4; // 4 Quarters
            }
        }
    }

    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("GAME_TIMER_UPDATE".equals(action)) {
                long timeRemaining = intent.getLongExtra("TIME_REMAINING", 0);
                int currentPeriod = intent.getIntExtra("CURRENT_PERIOD", 1);

                // Calculate minutes and seconds from timeRemaining
                long seconds = (timeRemaining / 1000) % 60;
                long minutes = (timeRemaining / (1000 * 60)) % 60;

                // Format the time as MM:SS
                timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

                // Update the TextView's color and text
                if (minutes < 1) {
                    tvTimeRem.setTextColor(Color.rgb(200, 0, 0)); // Red for less than a minute
                } else {
                    tvTimeRem.setTextColor(Color.BLACK); // Default color
                }
                tvTimeRem.setText(timeFormatted);

                //tvQuarterNum.setText("Quarter: " + currentPeriod);

                if (iNumPers == 2) {
                    tvQuarterNum.setText("Half:\n" + currentPeriod);
                } else {
                    tvQuarterNum.setText("Quarter:\n" + currentPeriod);
                }
            } else if ("END_OF_PERIOD_ACTION".equals(action)) {
                int currentPeriod = intent.getIntExtra("CURRENT_PERIOD", 1);
                int iNumPers = intent.getIntExtra("TOTAL_PERIODS", 4);
                Toast.makeText(context, "Period " + currentPeriod + " has ended.", Toast.LENGTH_SHORT).show();
                EndOfPeriodTimer();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        spSavedValues = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);

        SharedPreferences.Editor spEditor = spSavedValues.edit();

        spEditor.putInt("iScore1", iScore1);
        spEditor.putInt("iScore2", iScore2);
        spEditor.putInt("iGS1", iGS1);
        spEditor.putInt("iGA1", iGA1);
        spEditor.putInt("iGS1M", iGS1M);
        spEditor.putInt("iGA1M", iGA1M);
        spEditor.putInt("iGS2", iGS2);
        spEditor.putInt("iGA2", iGA2);
        spEditor.putInt("iGS2M", iGS2M);
        spEditor.putInt("iGA2M", iGA2M);
        spEditor.putString("tvTeam1", tvTeam1.getText().toString());
        spEditor.putString("etTeam2", etTeam2.getText().toString());
        spEditor.putString("sGSPlayer", sGSPlayer);
        spEditor.putString("sGAPlayer", sGAPlayer);

        String gameLogForPrefs = viewModel.getCurrentActionsLogString();
        spEditor.putString("GameLog", gameLogForPrefs); // Not using what Gemini suggested: mySharedPref.setAllActions(gameLogForPrefs);
        //spEditor.putString("sAllActions",sAllActions.toString()); //This what what i had before

        spEditor.putBoolean("GameInProgress", viewModel.getGameInProgress().getValue());
        spEditor.putString("GameMode", viewModel.getGameMode().getValue());
        spEditor.putString("CurrPeriod", String.valueOf(viewModel.getCurrentPeriod().getValue()));

        GameStats stats = new GameStats(
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()),
                tvTeam1.getText().toString(), // Team 1 name
                etTeam2.getText().toString(), // Team 2 name
                iScore1,                      // Team 1 score
                iScore2,                      // Team 2 score
                gameLogForPrefs.toString()        // Game log
        );

        new Thread(() -> {
            AppDatabase db = MyApplication.getDatabase();
            db.gameStatsDao().insertGameStats(stats);
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        spSavedValues = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        iGS1 = spSavedValues.getInt("iGS1", 0);
        iGA1 = spSavedValues.getInt("iGA1", 0);
        iGS1M = spSavedValues.getInt("iGS1M", 0);
        iGA1M = spSavedValues.getInt("iGA1M", 0);
        iGS2 = spSavedValues.getInt("iGS2", 0);
        iGA2 = spSavedValues.getInt("iGA2", 0);
        iGS2M = spSavedValues.getInt("iGS2M", 0);
        iGA2M = spSavedValues.getInt("iGA2M", 0);
        sTeam1 = spSavedValues.getString("tvTeam1", sTeam1);
        sTeam2 = spSavedValues.getString("etTeam2", sTeam2);
        sGSPlayer = spSavedValues.getString("sGSPlayer", sGSPlayer);
        sGAPlayer = spSavedValues.getString("sGAPlayer", sGAPlayer);

        viewModel.setGameInProgress(spSavedValues.getBoolean("GameInProgress", viewModel.getGameInProgress().getValue()));
        viewModel.setGameMode(spSavedValues.getString("GameMode", viewModel.getGameMode().getValue()));
        viewModel.setCurrentPeriod(spSavedValues.getInt("CurrPeriod", 1));
        bTimerRunning = viewModel.getGameInProgress().getValue();

        String actionsLogFromPrefs = spSavedValues.getString("GameLog", null);//" .getAllActions();
        if ((viewModel.currentActions.getValue() == null || viewModel.currentActions.getValue().isEmpty()) &&
                actionsLogFromPrefs != null && !actionsLogFromPrefs.isEmpty()) {

            List<ScoringAttempt> restoredAttempts = new ArrayList<>();
            String[] lines = actionsLogFromPrefs.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                // You NEED a robust way to parse each 'line' back into a ScoringAttempt object.
                // This depends entirely on the format defined by your ScoringAttempt.toString().
                // Example: If ScoringAttempt.toString() produces "PlayerName,Position,Result,Timestamp"
                try {
                    String[] parts = line.split(",", -1); // Split by comma, -1 to keep trailing empty strings if any
                    if (parts.length == 4) { // Or whatever number of parts your toString() creates
                        // Assuming constructor: ScoringAttempt(playerName, playerPosition, isSuccessful, timestamp)
                        if (parts[2].trim() == "true") {
                            restoredAttempts.add(new ScoringAttempt(parts[0].trim(), parts[1].trim(), true, parts[3].trim()));
                        } else {
                            restoredAttempts.add(new ScoringAttempt(parts[0].trim(), parts[1].trim(), false, parts[3].trim()));
                        }
                        //restoredAttempts.add(new ScoringAttempt(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()));
                    } else {
                        Log.w("FragGameplay", "Could not parse action line: " + line);
                    }
                } catch (Exception e) { // Catch any parsing errors
                    Log.e("FragGameplay", "Error parsing action line from SharedPreferences: " + line, e);
                }
            }
            viewModel.updateAllActions(restoredAttempts); // Set the restored list in the ViewModel
        }
        //sAllActions.setLength(0);
        //sAllActions.append(spSavedValues.getString("sAllActions",sAllActions.toString()));


        if (viewModel.getGameMode().getValue() != "") {
            btnGameMode.setText(viewModel.getGameMode().getValue());
        } else {
            btnGameMode.setText("15m,4Q");
        }
    }
    void MakeitShake(long[] InputPattern) {
        Vibrator Vibe = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        Vibe.vibrate(InputPattern, -1);
    }

    private View.OnClickListener updateGameTitle() {
        String team1Name = tvTeam1.getText().toString();
        String team2Name = etTeam2.getText().toString();

        // Check if both names are available before setting the title
        if (!team1Name.isEmpty() && !team2Name.isEmpty()) {
            String gameTitle = team1Name + " vs " + team2Name;
            tvGameTitle.setText(gameTitle);
        } else if (!team1Name.isEmpty()) {
            // If only Team 1 name is available, show just Team 1 name
            tvGameTitle.setText(team1Name);
        } else if (!team2Name.isEmpty()) {
            // If only Team 2 name is available, show just Team 2 name
            tvGameTitle.setText(team2Name);
        } else {
            // If neither name is available, set a default or clear the text
            tvGameTitle.setText("Game Title"); // Or set to ""
        }
        return null;
    }

    private void setupUI(View view) {
        // Set up touch listener for non-text views to hide keyboard and clear focus.
        if (!(view instanceof EditText)) { // Check if the current view is NOT an EditText
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(); // Call the method to hide the keyboard
                    clearFocusFromEditTexts(); // Call the method to clear focus
                    return false; // Allow the touch event to continue to underlying views
                }
            });
        }

        // If a layout container, iterate through children and recursively call this method
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView); // Recursively call setupUI for child views
            }
        }
    }

    private void hideSoftKeyboard() {
        Activity activity = requireActivity();
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        // Find the currently focused view, so we can grab a binder from it
        View focusedView = activity.getCurrentFocus();
        // If no view currently has focus, create a new one, just so we can get a binder from it
        if (focusedView == null) {
            focusedView = new View(activity);
        }
        inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0); // Hide the keyboard
    }

    private void clearFocusFromEditTexts() {
        Activity activity = requireActivity();
        View focusedView = activity.getCurrentFocus();
        if (focusedView instanceof EditText) {
            focusedView.clearFocus(); // Clear focus from the focused EditText
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireContext().unregisterReceiver(timerReceiver);
    }

    private void startImageAnimation() {
        if (ivCentrePassCircle == null || endX == 0) { // Check if view and coordinates are ready
            return;
        }
        ivCentrePassCircle.setVisibility(View.VISIBLE);

        // Determine target coordinates based on the current direction
        float targetY = movingToEndLocation ? endY : startY; // If Y also changes
        // Create a new animator with proper duration for both transitions
        animatorY = ObjectAnimator.ofFloat(ivCentrePassCircle, "Y", ivCentrePassCircle.getY(), targetY);
        animatorY.setDuration(200); // Ensure smooth animation both ways
        animatorY.setInterpolator(new AccelerateDecelerateInterpolator());

        animatorY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                movingToEndLocation = !movingToEndLocation; // Flip direction for next movement
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorY.start();
    }
}

class PlayerShotStats {
    String playerName;
    String playerPosition;
    int goals;
    int misses;

    public PlayerShotStats(String position) {
        this.playerName = playerName;
        this.playerPosition = position;
        this.goals = 0;
        this.misses = 0;
    }

    public String playerName() {
        return playerName;
    }

    public String playerPosition() {
        return playerPosition;
    }

    public int getGoals() {
        return goals;
    }

    public void incrementGoals() {

        this.goals++;
    }

    public int getMisses() {
        return misses;
    }

    public void incrementMisses() {
        this.misses++;
    }

    @Override
    public String toString() {
        return "PlayerShotStats{" +
                "Player='" + playerName + '\'' +
                ", goals=" + goals +
                ", misses=" + misses +
                '}';
    }
}
// TODO     Use nice pretty icons
// TODO     Generate sub-out routines, keep track of players' in-game time; Add maybe an array to keep track
//     of player on and off times, and sum up total in-game time, maybe just a single string with all sub events.
// TODO     Add players' player (best on court function)
// TODO     Data to hold stats for each player position
// TODO     Allow swiping from one tab to the next
// TODO     Change Main Activity from buttons to tab layout
// TODO     Change away from and Disable Stats and TeamList when game timer is less than 10 seconds, to stop end-of period glitching


// Done:
// TO DO     Buzz for in-game button clicks, points, end of playing time
// TO DO     Going from a game-in-progress to Stats and back is fine, but going to TeamList disables the buttons.// TO DO     Backing out of a game in progress also disables the buttons.// TO DO     Add first name of player in GS and GA to button
// TO DO     Export saved games to file; only commit finalised games.
// TO DO     Fix up exports
// TO DO     Fix up Playerlist display (RecyclerView)
// TO DO     Implement reset game
// TO DO     Implement enabled/disabled buttons
// TO DO     Assign player name to positions
// TO DO     Implement quarter/Half timer (game mode and time); copy from other version
// TO DO     Save Game/app Data when exiting and reload when restarting
// TO DO     Bring chosen team name into Gameplay
// TO DO     Change colours for centre pass change
// TO DO     Add Game Mode and Period Number and total Periods to both SharedModelView and SharedPref, also to be reset by reset button.
// TO DO     Put end of period note in gamestats




