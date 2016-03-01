package com.example.han.pintu.model;

import android.graphics.Bitmap;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

/**
 * Created by han on 15-12-24.
 */
@Table("laopotable")
public class ImagePiece {

    @PrimaryKey(AssignType.AUTO_INCREMENT) @Column("_id") protected long id;
    @Column("index") private int index;  //当前是第几块,索引号,图片真正的位置
    @Column("bitmap") private Bitmap bitmap;  //指向当前图

    public ImagePiece() {
    }

    public ImagePiece(int index, Bitmap bitmap) {
        this.index = index;
        this.bitmap = bitmap;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public String toString() {
        return "ImagePiece{" +
                "index=" + index +
                ", bitmap=" + bitmap +
                '}';
    }
}
