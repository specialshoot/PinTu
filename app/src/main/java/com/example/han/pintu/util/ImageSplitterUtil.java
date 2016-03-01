package com.example.han.pintu.util;

import android.graphics.Bitmap;

import com.example.han.pintu.model.ImagePiece;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片切片工具类
 * Created by han on 15-12-24.
 */
public class ImageSplitterUtil {

    /**
     * @param bitmap 传入bitmap
     * @param piece 切成piece块
     * @return 返回列表
     */
    public static List<ImagePiece> splitImage(Bitmap bitmap,int piece){
        List<ImagePiece> imagePieces=new ArrayList<ImagePiece>();

        int width=bitmap.getWidth();
        int height=bitmap.getHeight();

        int pieceWidth=Math.min(width,height) / piece;//取图片长宽中最大值,除以块数,得到每一块宽度
        //切片
        for (int i=0;i<piece;i++){
            for(int j=0;j<piece;j++){
                ImagePiece imagePiece=new ImagePiece();
                imagePiece.setIndex((j+i*piece));   //设置每块的标号
                int x=j*pieceWidth;
                int y=i*pieceWidth;

                imagePiece.setBitmap(Bitmap.createBitmap(bitmap,x,y,pieceWidth,pieceWidth));    //每块bitmap取x,y点坐标的pieceWidth长宽的区域
                imagePieces.add(imagePiece);
            }
        }
        return imagePieces;
    }
}
