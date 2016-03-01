package com.example.han.pintu.view;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.han.pintu.R;
import com.example.han.pintu.model.ImagePiece;
import com.example.han.pintu.util.ImageSplitterUtil;
import com.example.han.pintu.util.SPUtils;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by han on 15-12-24.
 */
public class GamePintuLayout extends RelativeLayout implements View.OnClickListener {

    private int mColumn = 3;  //行数列数,初始值3行3列
    private int mPadding;   //容器的内边距
    private int mMargin = 3;    //每张小图之间的横纵距离,初始值为3
    private ImageView[] mGamePinTuItems;    //ImageView
    private int mItemWidth; //每个分割图片的宽度(高度与宽度一致)
    private Bitmap mBitmap; //游戏图片
    private List<ImagePiece> mItemBitmaps;  //分割后的图片数组
    private int mWidth; //游戏面板宽度
    private boolean once = false;   //标志位,防止onMeasure不断重绘
    //点击操作中
    private ImageView mFirst;   //第一张点击的图片
    private ImageView mSecond;  //第二张点击的图片

    private RelativeLayout mAnimLayout; //动画层
    private boolean isAniming = false;  //动画是否正在执行
    private boolean isGameSuccess;  //游戏是否成功
    private boolean isGameOver; //游戏是否失败
    private boolean isChangePicture = false;
    private int mLevel = 1;  //游戏level

    /**
     * 回调接口
     */
    public interface GamePintuListener {
        void nextLevel(int nextLevel);

        void timeChanged(int currentTime);

        void gameover();
    }

    public GamePintuListener mListener;

    /**
     * 设置接口回调
     *
     * @param mListener
     */
    public void setOnGamePintuListener(GamePintuListener mListener) {
        this.mListener = mListener;
    }

    private static final int TIME_CHANGED = 0x110;
    private static final int NEXT_LEVEL = 0x111;
    private boolean isTimeEnabled = false;    //是否启动计时时间(是否在规定时间内完成的设定)
    private int mTime = 0;  //游戏时间

    /**
     * 设置是否开启时间设定
     *
     * @param isTimeEnabled
     */
    public void setTimeEnabled(boolean isTimeEnabled) {
        this.isTimeEnabled = isTimeEnabled;
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIME_CHANGED:
                    if (isGameSuccess || isGameOver || isPause) {
                        return;
                    }

                    if (mListener != null) {
                        mListener.timeChanged(mTime);
                        if (mTime == 0) {
                            isGameOver = true;
                            mListener.gameover();
                            return;
                        }
                    }
                    mTime--;

                    mHandler.sendEmptyMessageDelayed(TIME_CHANGED, 1000);    //延迟一秒执行
                    break;
                case NEXT_LEVEL:
                    mLevel++;
                    if (mListener != null) {
                        mListener.nextLevel(mLevel); //进入下一关的接口
                    } else {
                        nextLevel();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public GamePintuLayout(Context context) {
        this(context, null);
    }

    public GamePintuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GamePintuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());  //转化为dp
        mPadding = min(getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom());
    }

    /**
     * 获取多个参数的最小值
     *
     * @param params
     * @return
     */
    private int min(int... params) {
        int min = params[0];
        for (int param : params) {
            if (param < min) {
                min = param;
            }
        }
        return min;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = Math.min(getMeasuredHeight(), getMeasuredWidth()); //取高宽中的小值
        Log.e("TAG","ONCE ONMEASURE -> "+once);
        if (!once) {
            initBitmap();   //进行切除及排序
            initItem(); //设置ImageView(Item)的宽高等属性
            //判断是否开启时间
            checkTimeEnable();
            once = true;
        }
        Log.e("TAG","ONCE ONMEASURE Last -> "+once);
        setMeasuredDimension(mWidth, mWidth);
    }

    private void checkTimeEnable() {
        if (isTimeEnabled) {
            countTimeBaseLevel();   //根据当前时间等级设置时间
            mHandler.sendEmptyMessage(TIME_CHANGED);
        }
    }

    private void countTimeBaseLevel() {
        Log.e("TAG", "mLevel -> " + mLevel);
        mTime = (int) Math.pow(2, mLevel) * 60;   //时间呈指数增长
    }

    /**
     * 初始化mItemBitmaps,进行切图与乱序操作
     */
    private void initBitmap() {
        /*********************看是否SharedPreferences已存图片与关数************************/
        Log.e("TAG","ONCE initBitmap -> "+once);
        if (!once) {
            try {
                if (!SPUtils.get(getContext(), "uri", "").equals("")) {
                    String uriString = (String) SPUtils.get(getContext(), "uri", "");
                    Uri uri = Uri.parse(uriString);
//                    ContentResolver cr = getContext().getContentResolver();
                    try {
//                        mBitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                        mBitmap= MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                    } catch (Exception e) {
                        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lover);
                    }
                }
                if ((int) (SPUtils.get(getContext(), "level", 0)) != 0) {
                    int level = (int) SPUtils.get(getContext(), "level", 0);
                    mLevel = level;
                    mColumn = level + 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /***************************************************************************/
        if (mBitmap == null) {
            Log.e("TAG","mBitmap==null");
            mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lover);
        }
        mItemBitmaps = ImageSplitterUtil.splitImage(mBitmap, mColumn);  //切图

        //使用sort实现乱序
        Collections.sort(mItemBitmaps, new Comparator<ImagePiece>() {
            @Override
            public int compare(ImagePiece lhs, ImagePiece rhs) {
                //乱序,random出现0-1之间的值,所以这里面不一定返回哪个
                return Math.random() > 0.5 ? 1 : -1;
            }
        });
    }

    /**
     * 设置ImageView(Item)的宽高等属性
     */
    private void initItem() {
        mItemWidth = (mWidth - mPadding * 2 - mMargin * (mColumn - 1)) / mColumn; //(容器宽度-内边距-item间的边距)/每行个数
        mGamePinTuItems = new ImageView[mColumn * mColumn];
        //生成item,设置Rule
        for (int i = 0; i < mGamePinTuItems.length; i++) {
            ImageView item = new ImageView(getContext());
            item.setOnClickListener(this);  //点击方法

            item.setImageBitmap(mItemBitmaps.get(i).getBitmap());
            mGamePinTuItems[i] = item;
            item.setId(i + 1);    //设置id
            item.setTag(i + "_" + mItemBitmaps.get(i).getIndex());  //设置这种tag格式在点击操作时有用

            LayoutParams lp = new LayoutParams(mItemWidth, mItemWidth);
            //设置item横向间隙,rightMargin右边距
            //不是最后一列
            if ((i + 1) % mColumn != 0) {
                lp.rightMargin = mMargin;
            }
            //不是第一列
            if (i % mColumn != 0) {
                lp.addRule(RelativeLayout.RIGHT_OF, mGamePinTuItems[i - 1].getId());    //设置规则,距离右边id为mGamePinTuItems[i - 1].getId()控件为上面的margin值
            }
            //如果不是第一行,设置topmargin和rule
            if ((i + 1) > mColumn) {
                lp.topMargin = mMargin;
                lp.addRule(RelativeLayout.BELOW, mGamePinTuItems[i - mColumn].getId());
            }
            addView(item, lp);
        }
    }

    @Override
    public void onClick(View v) {

        if (isAniming) {
            //正在动画的时候点击应该无效果
            return;
        }
        if (mFirst == v) {
            //第二次点击同一张图片
            mFirst.setColorFilter(null);
            mFirst = null;
            return;
        }

        if (mFirst == null) {
            //第一次点击
            mFirst = (ImageView) v;
            mFirst.setColorFilter(Color.parseColor("#55ff0000"));
        } else {
            //第二次点击
            mSecond = (ImageView) v;
            exchangeView(); //交换两张图片位置
        }
    }

    /**
     * 交换两张图片位置
     */
    private void exchangeView() {
        mFirst.setColorFilter(null);    //去掉选中颜色状态

        //构造动画层
        setUpAnimLayout();

        ImageView first = new ImageView(getContext());
        final Bitmap firstBitmap = mItemBitmaps.get(getImageIdByTag((String) mFirst.getTag())).getBitmap();
        first.setImageBitmap(firstBitmap);
        LayoutParams lp = new LayoutParams(mItemWidth, mItemWidth);
        lp.leftMargin = mFirst.getLeft() - mPadding;
        lp.topMargin = mFirst.getTop() - mPadding;
        first.setLayoutParams(lp);
        mAnimLayout.addView(first);

        ImageView second = new ImageView(getContext());
        final Bitmap secondBitmap = mItemBitmaps.get(getImageIdByTag((String) mSecond.getTag())).getBitmap();
        second.setImageBitmap(secondBitmap);
        LayoutParams lp2 = new LayoutParams(mItemWidth, mItemWidth);
        lp2.leftMargin = mSecond.getLeft() - mPadding;
        lp2.topMargin = mSecond.getTop() - mPadding;
        second.setLayoutParams(lp2);
        mAnimLayout.addView(second);

        //设置动画
        TranslateAnimation anim = new TranslateAnimation(0, mSecond.getLeft() - mFirst.getLeft(), 0, mSecond.getTop() - mFirst.getTop());
        anim.setDuration(300);
        anim.setFillAfter(true);
        first.startAnimation(anim);

        TranslateAnimation anim2 = new TranslateAnimation(0, mFirst.getLeft() - mSecond.getLeft(), 0, mFirst.getTop() - mSecond.getTop());
        anim2.setDuration(300);
        anim2.setFillAfter(true);
        second.startAnimation(anim2);

        //动画监听
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mFirst.setVisibility(View.INVISIBLE);
                mSecond.setVisibility(View.INVISIBLE);

                isAniming = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                String firstTag = (String) mFirst.getTag();
                String secondTag = (String) mSecond.getTag();

                mSecond.setImageBitmap(firstBitmap);
                mFirst.setImageBitmap(secondBitmap);

                mFirst.setTag(secondTag);
                mSecond.setTag(firstTag);

                mFirst.setVisibility(View.VISIBLE);
                mSecond.setVisibility(View.VISIBLE);
                mFirst = mSecond = null;
                mAnimLayout.removeAllViews();   //动画结束移除所有动画层上的组件

                //判断用户是否拼完拼图
                checkSuccess();
                isAniming = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * 判断用户是否拼完拼图
     */
    private void checkSuccess() {
        boolean isSuccess = true;
        for (int i = 0; i < mGamePinTuItems.length; i++) {
            ImageView imageView = mGamePinTuItems[i];
            if (getImageIndexByTag((String) imageView.getTag()) != i) {
                isSuccess = false;
            }
        }

        Log.e("TAG", "isSuccess -> " + isSuccess);
        if (isSuccess) {
            isGameSuccess = true;
            mHandler.removeMessages(TIME_CHANGED);  //成功后不必在执行时间减一操作

            Log.e("TAG", "Success , level up!!!");
            Toast.makeText(getContext(), "Success , level up!!!", Toast.LENGTH_SHORT).show();
            mHandler.sendEmptyMessage(NEXT_LEVEL);  //交给handler去处理
        }
    }

    /**
     * 根据tag获取id
     *
     * @param tag
     * @return
     */
    public int getImageIdByTag(String tag) {
        String[] split = tag.split("_");
        return Integer.parseInt(split[0]);
    }

    /**
     * 根据tag获取图片index
     *
     * @param tag
     * @return
     */
    public int getImageIndexByTag(String tag) {
        String[] split = tag.split("_");
        return Integer.parseInt(split[1]);
    }

    /**
     * 构造动画层
     */
    private void setUpAnimLayout() {
        if (mAnimLayout == null) {
            mAnimLayout = new RelativeLayout(getContext());
            addView(mAnimLayout);
        }
    }

    /**
     * 设置进入下一关
     */
    public void nextLevel() {
        this.removeAllViews();
        mAnimLayout = null;
        mColumn++;
        isGameSuccess = false;
        checkTimeEnable();
        initBitmap();
        initItem();
    }

    /**
     * 重新开始
     */
    public void restart() {
        isGameOver = false;
        mColumn--;
        nextLevel();
    }

    /**
     * 重新开始
     */
    public void restartWin() {
        isGameOver = false;
        mLevel--;
        mColumn--;
        nextLevel();
    }

    private boolean isPause = false;

    public void pause() {
        isPause = true;
        mHandler.removeMessages(TIME_CHANGED);
    }

    public void resume() {
        if (isPause) {
            isPause = false;
            if (!isChangePicture) {
                mHandler.sendEmptyMessage(TIME_CHANGED);
            } else {
                isChangePicture = false;
            }
        }
    }

    public void setBitmap(Bitmap bitmap) {
        isChangePicture = true;
        isGameSuccess = false;
        isGameOver = false;
        mColumn = 3;
        mLevel = 1;
        this.mBitmap = bitmap;
        restart();
    }

    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int mLevel) {
        mHandler.removeMessages(TIME_CHANGED);
        isGameSuccess = false;
        isGameOver = false;
        this.mLevel = mLevel;
        this.mColumn = mLevel + 2;
        restart();
    }
}
