/**
 * RectF.java
 */
package com.polites.android;

/**
 * @author Di Zhang Jan 10, 201311:29:31 AM
 */
public class RectF extends android.graphics.RectF{

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RectF r = (RectF) o;
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
    }
}
