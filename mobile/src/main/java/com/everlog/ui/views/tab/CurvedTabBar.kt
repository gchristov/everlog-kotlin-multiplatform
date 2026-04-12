package com.everlog.ui.views.tab

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.utils.ViewUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

class CurvedTabBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val sideOffset = ViewUtils.dpToPx(1)
    private var fabRadius = ViewUtils.dpToPx(80) / 2

    private val mPath = Path()
    private val mPaintFill = Paint()
    private val mPaintStroke = Paint()

    private val mFirstCurveStartPoint = Point()
    private val mFirstCurveEndPoint = Point()
    private val mFirstCurveControlPoint1 = Point()
    private val mFirstCurveControlPoint2 = Point()

    private var mSecondCurveStartPoint = Point()
    private val mSecondCurveEndPoint = Point()
    private val mSecondCurveControlPoint1 = Point()
    private val mSecondCurveControlPoint2 = Point()

    private var mNavigationBarWidth = 0
    private var mNavigationBarHeight = 0

    init {
        // Background
        with(mPaintFill) {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.background_toolbar)
        }
        // Stroke
        with(mPaintStroke) {
            strokeWidth = sideOffset.toFloat()
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.separator)
        }
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Get width and height of navigation bar
        mNavigationBarWidth = width + sideOffset
        mNavigationBarHeight = height + sideOffset
        // The coordinates (x,y) of the start point before curve
        mFirstCurveStartPoint.set(mNavigationBarWidth / 2 - fabRadius * 2 - fabRadius / 8, 0)
        // The coordinates (x,y) of the end point after curve
        mFirstCurveEndPoint.set(mNavigationBarWidth / 2, fabRadius + fabRadius / 4)
        // Same thing for the second curve
        mSecondCurveStartPoint = mFirstCurveEndPoint
        mSecondCurveEndPoint.set(mNavigationBarWidth / 2 + fabRadius * 2 + fabRadius / 8, 0)

        // The coordinates (x,y)  of the 1st control point on a cubic curve
        mFirstCurveControlPoint1.set(
                mFirstCurveStartPoint.x + fabRadius + fabRadius / 4,
                mFirstCurveStartPoint.y
        )
        // The coordinates (x,y)  of the 2nd control point on a cubic curve
        mFirstCurveControlPoint2.set(
                mFirstCurveEndPoint.x - fabRadius * 2 + fabRadius,
                mFirstCurveEndPoint.y
        )

        mSecondCurveControlPoint1.set(
                mSecondCurveStartPoint.x + fabRadius * 2 - fabRadius,
                mSecondCurveStartPoint.y
        )
        mSecondCurveControlPoint2.set(
                mSecondCurveEndPoint.x - (fabRadius + fabRadius / 4),
                mSecondCurveEndPoint.y
        )
        // Apply changes to path
        mPath.apply {
            reset()
            moveTo(0f - sideOffset, 0f)
            lineTo(mFirstCurveStartPoint.x.toFloat(), mFirstCurveStartPoint.y.toFloat())
            cubicTo(
                    mFirstCurveControlPoint1.x.toFloat(), mFirstCurveControlPoint1.y.toFloat(),
                    mFirstCurveControlPoint2.x.toFloat(), mFirstCurveControlPoint2.y.toFloat(),
                    mFirstCurveEndPoint.x.toFloat(), mFirstCurveEndPoint.y.toFloat()
            )
            cubicTo(
                    mSecondCurveControlPoint1.x.toFloat(), mSecondCurveControlPoint1.y.toFloat(),
                    mSecondCurveControlPoint2.x.toFloat(), mSecondCurveControlPoint2.y.toFloat(),
                    mSecondCurveEndPoint.x.toFloat(), mSecondCurveEndPoint.y.toFloat()
            )
            lineTo(mNavigationBarWidth.toFloat(), 0f)
            lineTo(mNavigationBarWidth.toFloat(), mNavigationBarHeight.toFloat())
            lineTo(0f, mNavigationBarHeight.toFloat())
            close()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(mPath, mPaintFill)
        canvas.drawPath(mPath, mPaintStroke)
    }
}