package com.project.playtimerank;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.project.playtimerank.data.FollowAdapter;
import com.project.playtimerank.data.FollowInfo;
import com.project.playtimerank.db.DBHelper;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button followBtn;
    private EditText userNameEditText;
    private RecyclerView playTimeRankRecyclerView;
    private Button calRankBtn;
    private final String apiKey = "{Riot Developer 에서 제공하는 api key}";
    private final DBHelper helper = new DBHelper(this);
    private final LinearLayoutManager layoutManager = new LinearLayoutManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playTimeRankRecyclerView = findViewById(R.id.play_time_rank_recyclerview);
        playTimeRankRecyclerView.setLayoutManager(layoutManager);

        userNameEditText = findViewById(R.id.username_edittext);

        followBtn = findViewById(R.id.follow_btn);
        followBtn.setOnClickListener(this);

        calRankBtn = findViewById(R.id.calculate_rank_button);
        calRankBtn.setOnClickListener(this);
    }

    private JsonReader requestJsonReader(URL requestURL) throws Exception{
        Log.i("my", "url : " + requestURL);
        JsonReader jsonReader = null;

        try {
            HttpURLConnection httpURLConnection
                    = (HttpURLConnection) requestURL.openConnection();
            if (httpURLConnection.getResponseCode() == 200) {
                InputStream responseBody = httpURLConnection.getInputStream();
                InputStreamReader responseBodyReader
                        = new InputStreamReader(responseBody, "UTF-8");

                jsonReader = new JsonReader(responseBodyReader);
            }
        } catch (Exception e) {
            Log.i("my", e.toString());
            throw new Exception();
        }

        return jsonReader;
    }

    private void requestPuuid(String userName) {
        AsyncTask.execute(new Runnable() {

            String puuid;
            boolean isErrorOccur = false;

            @Override
            public void run() {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/")
                            .append(userName)
                            .append("?api_key=")
                            .append(apiKey);
                    URL requestURL = new URL(new String(sb));
                    JsonReader jsonReader = requestJsonReader(requestURL);
                    jsonReader.beginObject();

                    while (jsonReader.hasNext()) {
                        String key = jsonReader.nextName();
                        if (key.equals("puuid")) {
                            puuid = jsonReader.nextString();
                        } else {
                            jsonReader.skipValue();
                        }
                    }

                    SQLiteDatabase db = helper.getWritableDatabase();
                    db.execSQL("insert into tb_follows (username, puuid) values (?, ?)",
                            new String[]{userName, puuid});

                } catch (NullPointerException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "잘못된 요청입니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    isErrorOccur = true;
                } catch (SQLiteConstraintException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "이미 팔로우 했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    isErrorOccur = true;
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        Log.i("my", e.toString());
                    }
                    });
                    isErrorOccur = true;
                }
                if(!isErrorOccur) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "팔로우 성공", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private List<String> getMatchOfFollows(String puuid) throws Exception {

        List<String> matchList = new ArrayList<>();
        Date date = new Date();
        long curUnixTime = date.getTime() / 1000L;

        StringBuilder sb = new StringBuilder();
        sb.append("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/")
                .append(puuid)
                .append("/ids?start=0&count=100")
                .append("&startTime=")
                .append(curUnixTime - 604800)
                .append("&api_key=")
                .append(apiKey);

        try {
            URL requestUrl = new URL(new String(sb));
            JsonReader jsonReader = requestJsonReader(requestUrl);
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                matchList.add(jsonReader.nextString());
            }
        } catch (Exception e) {
            Log.i("my" ,"Error in calculate time of follows");
        }
        return matchList;
    }

    private int calculateTimeOfMatches(List<String> matches) throws Exception{
        int totalTime = 0;

        for(String match : matches) {
            StringBuilder sb = new StringBuilder();
            sb.append("https://asia.api.riotgames.com/lol/match/v5/matches/")
                    .append(match)
                    .append("?api_key=")
                    .append(apiKey);
            URL requestUrl = new URL(new String(sb));
            JsonReader jsonReader = requestJsonReader(requestUrl);
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String key = jsonReader.nextName();
                    if (key.equals("info")) {
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) {
                            String infoKey = jsonReader.nextName();
                            if (infoKey.equals("gameDuration")) {
                                totalTime += jsonReader.nextInt();
                                break;
                            } else {
                                jsonReader.skipValue();
                            }
                        }
                    } else {
                        jsonReader.skipValue();
                    }
                }
            } catch (NullPointerException e) {

            }
        }
        return totalTime / 60;
    }

    @Override
    public void onClick(View view) {
        if(view == followBtn) {
            String userName = userNameEditText.getText().toString();
            if(userName.equals("")) return;
            userNameEditText.setText("");
            requestPuuid(userName);

        } else if(view == calRankBtn) {
            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Loading...");
            progressDialog.show();

            List<FollowInfo> followInfoList = new ArrayList<>();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    SQLiteDatabase db = helper.getReadableDatabase();
                    Cursor cursor = db.rawQuery("select username, puuid from tb_follows", null);
                    while(cursor.moveToNext()) {
                        String puuid = cursor.getString(1);
                        try {
                            List<String> matches = getMatchOfFollows(puuid);

                            int totalTime = 0;
                            if(matches.size() != 0) {
                                Log.i("my", "size is not 0");
                                totalTime = calculateTimeOfMatches(matches);
                            }
                            followInfoList.add(new FollowInfo(
                                    cursor.getString(0),
                                    cursor.getString(1),
                                    totalTime
                            ));
                            Log.i("my",
                                    "username : " + cursor.getString(0) +
                                            " puuid : " + cursor.getString(1) +
                                            " totalTime : " + totalTime);
                        } catch (Exception e) {

                        }
                    }
                    Collections.sort(followInfoList);
                    Log.i("my", "실행");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Log.i("my", "size : " + followInfoList.size());
                            playTimeRankRecyclerView.setAdapter(new FollowAdapter(MainActivity.this, followInfoList));
                        }
                    });
                }
            });
        }
    }
}