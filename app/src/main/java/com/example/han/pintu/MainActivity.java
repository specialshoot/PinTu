package com.example.han.pintu;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.han.pintu.util.SPUtils;
import com.example.han.pintu.view.CustomDialog;
import com.example.han.pintu.view.GamePintuLayout;
import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;

import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private GamePintuLayout mGamePintuLayout;
    private TextView mLevel;
    private TextView mTime;
    private TextView mBest;
    private ImageView mBack;
    private ImageView mHelp;
    private Button mChoice;
    private Button mChooseLevel;
    private int mLevelValue = 1;
    private static Boolean isExit = false;    //判断是否第一次点击退出

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initView();
        initEvent();
    }

    private void initView() {
        mGamePintuLayout = (GamePintuLayout) findViewById(R.id.id_pintu);
        mTime = (TextView) findViewById(R.id.id_time);
        mLevel = (TextView) findViewById(R.id.id_level);
        mBest = (TextView) findViewById(R.id.id_bestScore);
        mChoice = (Button) findViewById(R.id.id_choice);
        mChooseLevel = (Button) findViewById(R.id.id_setLevel);
        mBack = (ImageView) findViewById(R.id.back);
        mHelp = (ImageView) findViewById(R.id.help);
    }

    private void initEvent() {
        mBest.setText("best : " + (int) SPUtils.get(MainActivity.this, "best", 1));
        mLevelValue = (int) SPUtils.get(MainActivity.this, "level", 1);
        mLevel.setText("关卡 : " + mLevelValue);
        mChoice.setOnClickListener(this);
        mChooseLevel.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mHelp.setOnClickListener(this);
        mGamePintuLayout.setTimeEnabled(true);

        mGamePintuLayout.setOnGamePintuListener(new GamePintuLayout.GamePintuListener() {
            @Override
            public void nextLevel(final int nextLevel) {

                //保存最好成绩
                SPUtils.put(MainActivity.this, "level", mGamePintuLayout.getLevel());
                int bestScore = (int) SPUtils.get(MainActivity.this, "best", 1);
                if (nextLevel > bestScore) {
                    SPUtils.put(MainActivity.this, "best", nextLevel);
                    mBest.setText("best : " + nextLevel);
                }

                final NiftyDialogBuilder dialogBuilder = NiftyDialogBuilder.getInstance(MainActivity.this);

                dialogBuilder
                        .withTitle("老婆真棒!!!")                                  //.withTitle(null)  no title
                        .withTitleColor("#FFFFFF")                                  //def
                        .withDividerColor("#11000000")                              //def
                        .withMessage("老婆真聪明，老婆我爱你!!!")                     //.withMessage(null)  no Msg
                        .withMessageColor("#FFFFFFFF")                              //def  | withMessageColor(int resid)
                        .withDialogColor("#FFE74C3C")                               //def  | withDialogColor(int resid)                               //def
                        .withIcon(getResources().getDrawable(R.drawable.github_icon))
                        .isCancelableOnTouchOutside(true)                           //def    | isCancelable(true)
                        .withDuration(700)                                          //def
                        .withEffect(Effectstype.Fliph)                                         //def Effectstype.Slidetop
                        .withButton1Text("我要继续!!!")                                      //def gone
                        .withButton2Text("回味这一局")                                  //def gone
                        .setCustomView(R.layout.nifty_view, MainActivity.this)         //.setCustomView(View or ResId,context)
                        .setButton1Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mGamePintuLayout.nextLevel();
                                mLevel.setText("关卡 : " + nextLevel);
                                mLevelValue = nextLevel;
                                SPUtils.put(MainActivity.this, "level", mGamePintuLayout.getLevel());
                                dialogBuilder.dismiss();
                            }
                        })
                        .setButton2Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mGamePintuLayout.restartWin();
                                dialogBuilder.dismiss();
                            }
                        })
                        .show();
            }

            @Override
            public void timeChanged(int currentTime) {
                mTime.setText("剩余时间 : " + currentTime);
            }

            @Override
            public void gameover() {
                final NiftyDialogBuilder dialogBuilder = NiftyDialogBuilder.getInstance(MainActivity.this);

                dialogBuilder
                        .withTitle("老婆输了不要紧")
                        .withTitleColor("#FFFFFF")
                        .withDividerColor("#11000000")
                        .withMessage("老婆输了是我的错,呜呜")
                        .withMessageColor("#FFFFFFFF")
                        .withDialogColor("#FFE74C3C")
                        .withIcon(getResources().getDrawable(R.drawable.github_icon))
                        .isCancelableOnTouchOutside(true)
                        .withDuration(700)
                        .withEffect(Effectstype.Fliph)
                        .withButton1Text("老婆还能赢")
                        .withButton2Text("什么破游戏,不玩儿了")
                        .setCustomView(R.layout.nifty_view, MainActivity.this)
                        .setButton1Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mGamePintuLayout.restart();
                                dialogBuilder.dismiss();
                            }
                        })
                        .setButton2Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                                dialogBuilder.dismiss();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_choice:
                Intent intent = new Intent();
                /* 开启Pictures画面Type设定为image */
                intent.setType("image/*");
                /* 使用Intent.ACTION_GET_CONTENT这个Action */
                intent.setAction(Intent.ACTION_GET_CONTENT);
                /* 取得相片后返回本画面 */
                startActivityForResult(intent, 1);
                break;
            case R.id.id_setLevel:
                final CustomDialog.Builder builder = new CustomDialog.Builder(this);
                builder.setTitle("选择关卡");
                builder.setSeekbarValue(mLevelValue);
                builder.setPositiveButton("就这关了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int value = builder.getSeekbarValue();
                        System.out.println("getSeekbarValue() -> " + value);
                        mGamePintuLayout.setLevel(value);
                        mLevelValue = value;
                        mLevel.setText("关卡 : " + mLevelValue);
                        SPUtils.put(MainActivity.this, "level", mLevelValue);
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                break;
            case R.id.back:
                exitBy2Click();
                break;
            case R.id.help:
                final NiftyDialogBuilder dialogBuilder=NiftyDialogBuilder.getInstance(this);
                dialogBuilder
                        .withTitle("游戏说明")                                  //.withTitle(null)  no title
                        .withTitleColor("#FFFFFF")                                  //def
                        .withDividerColor("#11000000")                              //def
                        .withMessage("点击两张图片可以交换顺序,两次点击同一张图片可以取消点击,点击选择关卡可以选择从第一关到当前所赢得的最好成绩关卡,点击选择图片来更换图片.注意:更换图片后最好成绩将置为第一关!!!")                     //.withMessage(null)  no Msg
                        .withMessageColor("#FFFFFFFF")                              //def  | withMessageColor(int resid)
                        .withDialogColor("#FFE74C3C")                               //def  | withDialogColor(int resid)
                        .withIcon(getResources().getDrawable(R.drawable.github_icon))
                        .withDuration(700)                                          //def
                        .withEffect(Effectstype.Newspager)                                         //def Effectstype.Slidetop
                        .withButton1Text("知道了")                                      //def gone
                        .isCancelableOnTouchOutside(true)                           //def    | isCancelable(true)
                        .setButton1Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialogBuilder.dismiss();
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                mGamePintuLayout.setBitmap(bitmap);
                SPUtils.put(MainActivity.this, "uri", uri.toString());
                SPUtils.put(MainActivity.this, "level", 1);
                SPUtils.put(MainActivity.this, "best", 1);
                mLevelValue = 1;
                mLevel.setText("关卡 : " + mLevelValue);
                mBest.setText("best : " + mLevelValue);
            } catch (FileNotFoundException e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGamePintuLayout.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGamePintuLayout.resume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitBy2Click();
        }
        return false;
    }

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true;
            Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;   //取消退出
                }
            }, 2000);    //等待2秒钟
        } else {
            finish();
        }
    }
}
