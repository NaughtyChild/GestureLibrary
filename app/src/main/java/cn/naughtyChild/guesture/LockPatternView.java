package cn.naughtyChild.guesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * @Title: LockPatternView
 * @Description: 手势锁
 * @date 2016/5/23 12:59
 * @auther fan
 */
public class LockPatternView extends View {
    private static final String TAG = "LockPatternView";
    private float movingX, movingY;
    private int width, height;
    private int cellRadius, cellInnerRadius, middleRadius;
    //in stealth mode (default is false),if set true,the gesture is invisible
    private boolean mInStealthMode = false;
    //haptic feed back (default is true),if set true,it'll shake when gesture drawing.
    private boolean mEnableHapticFeedback = true;
    //set offset to the boundary
    private int offset = 10;//left and right spacing
    private DisplayMode mode = DisplayMode.NORMAL;//default  mode
    private int outerCircleColor, selectInnerColor, errorInnerColor, errorMiddleColor, selectMiddleColor;
    private Paint outerCirclePaint, middleCirclePaint, innerCirclePaint, linePaint;
    private Cell[][] mCells = new Cell[3][3];
    private List<Cell> sCells = new ArrayList<Cell>();
    private OnPatternListener patterListener;
    private Context context;

    public LockPatternView(Context context) {
        this(context, null);
    }

    public LockPatternView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockPatternView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.init();
    }

    public int getOuterCircleColor() {
        return outerCircleColor;
    }

    public void setOuterCircleColor(int outerCircleColor) {
        this.outerCircleColor = outerCircleColor;
    }

    public int getSelectInnerColor() {
        return selectInnerColor;
    }

    public void setSelectInnerColor(int selectInnerColor) {
        this.selectInnerColor = selectInnerColor;
    }

    public int getSelectMiddleColor() {
        return selectMiddleColor;
    }

    public void setSelectMiddleColor(int selectMiddleColor) {
        this.selectMiddleColor = selectMiddleColor;
    }

    /**
     * initialize
     */
    private void init() {
        outerCircleColor = context.getResources().getColor(R.color.gray);
        selectInnerColor = context.getResources().getColor(R.color.mediumaquamarine);
        selectMiddleColor = context.getResources().getColor(R.color.darkgreen);
        errorInnerColor = context.getResources().getColor(R.color.indianred);
        errorMiddleColor = context.getResources().getColor(R.color.palevioletred);
        this.init9Cells();
        this.initPaints();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.width = getMeasuredWidth();
        this.height = getMeasuredHeight();
        if (width != height) {
            throw new IllegalArgumentException("the width must be equals height");
        }
        this.initCellSize();
        this.set9CellsSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("LockPatternView", "onDraw: ");
        switch (mode) {
            case NORMAL:
                //初始状态绘制
                drawNormalCircle(canvas);
                break;
            case DOWN:
                drawDownCircle(canvas);
                break;
            case MOVING:
                drawMovingCircle(canvas);
                break;
            case SUCCESS:
                drawSuccessResult(canvas);
                break;
            case ERROR:
                drawErrorResult(canvas);
                break;
        }
    }

    private void drawDownCircle(Canvas canvas) {
        drawMovingCircle(canvas);
    }

    private void drawErrorResult(Canvas canvas) {
        drawUPResult(canvas);
    }

    private void drawSuccessResult(Canvas canvas) {
        drawUPResult(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float ex = event.getX();
        float ey = event.getY();
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                handleActionDown(ex, ey);
                Log.d("LockPatternView", "onTouchEvent:ACTION_DOWN ");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("LockPatternView", "onTouchEvent:ACTION_MOVE ");
                handleActionMove(ex, ey);
                break;
            case MotionEvent.ACTION_UP:
                Log.d("LockPatternView", "onTouchEvent:ACTION_UP ");
                handleActionUp();
                break;
        }
        return true;
    }

    public void resetMode() {
        Log.d("LockPatternView", "restMode: ");
        postDelayed(new Runnable() {
            @Override
            public void run() {
                sCells.clear();
                for (Cell[] mCell : mCells) {
                    for (Cell cell : mCell) {
                        cell.status = STATUS.STATE_NORMAL;
                    }
                }
                mode = DisplayMode.NORMAL;
                invalidate();
            }
        }, 1000);
    }

    private void drawUPResult(Canvas canvas) {
        for (int i = 0; i < mCells.length; i++) {
            for (int j = 0; j < mCells[i].length; j++) {
                Cell cell = mCells[i][j];
                switch (cell.status) {
                    case STATE_NORMAL:
                        //正常时候，绘制外圆
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellRadius, this.outerCirclePaint);
                        break;
                    case STATE_CHECK_ERROR:
                        //判定错误时候
                        middleCirclePaint.setColor(errorMiddleColor);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.middleCirclePaint);
                        innerCirclePaint.setColor(errorInnerColor);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.innerCirclePaint);
                        break;
                    case STATE_CHECK:
                        middleCirclePaint.setColor(selectMiddleColor);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.middleCirclePaint);
                        innerCirclePaint.setColor(errorInnerColor);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.innerCirclePaint);
                        break;
                    case STATE_CHECK_SUCCESS:
                        // TODO: 2019/11/1
                        //判定正确时候
                        middleCirclePaint.setColor(context.getResources().getColor(R.color.limegreen));
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.middleCirclePaint);
                        innerCirclePaint.setColor(context.getResources().getColor(R.color.limegreen));
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.innerCirclePaint);
                        break;
                }
            }
        }
        drawLines(canvas);
    }


    private void drawMovingCircle(Canvas canvas) {
        for (int i = 0; i < mCells.length; i++) {
            for (int j = 0; j < mCells[i].length; j++) {
                Cell cell = mCells[i][j];
                switch (cell.status) {
                    case STATE_NORMAL:
                        //正常时候，绘制外圆
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellRadius, this.outerCirclePaint);
                        break;
                    case STATE_CHECK:
                        middleCirclePaint.setColor(selectMiddleColor);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.middleCirclePaint);
                        innerCirclePaint.setColor(selectInnerColor);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.innerCirclePaint);
                        break;
                }
            }
        }
        drawLines(canvas);
    }

    private void drawLines(Canvas canvas) {
        if (sCells.size() > 0) {
            //temporary cell: at the beginning the cell is the first of sCells
            Cell tempCell = sCells.get(0);
            for (int i = 1; i < sCells.size(); i++) {
                Cell cell = sCells.get(i);
                if (cell.getStatus() == STATUS.STATE_CHECK) {
                    drawLineNotIncludeInnerCircle(tempCell, cell, canvas, innerCirclePaint);
                } else if (cell.getStatus() == STATUS.STATE_CHECK_ERROR) {
                    drawLineNotIncludeInnerCircle(tempCell, cell, canvas, innerCirclePaint);
                }
                tempCell = cell;
            }
            if (mode == DisplayMode.MOVING) {
                drawLineFollowFingerNotIncludeInner(tempCell, canvas, innerCirclePaint);
            }
        }
    }

    private void drawNormalCircle(Canvas canvas) {
        for (int i = 0; i < mCells.length; i++) {
            for (int j = 0; j < mCells[i].length; j++) {
                //正常时候，绘制外圆
                outerCirclePaint.setColor(outerCircleColor);
                canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellRadius, this.outerCirclePaint);
            }
        }
    }

    /**
     * initialize cell size (include circle radius, inner circle radius,
     * cell box width, cell box height)
     */
    private void initCellSize() {
        //间距是一个半径
        this.cellRadius = (this.width - offset * 2) / 8;
        this.cellInnerRadius = this.cellRadius / 3;
        this.middleRadius = this.cellRadius - 3;
    }

    /**
     * initialize nine cells
     */
    private void init9Cells() {
        //the distance between the center of two circles
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mCells[i][j] = new Cell(0, 0, i, j, 3 * i + j + 1);
            }
        }
    }

    /**
     * set nine cells size
     */
    private void set9CellsSize() {
        int distance = 3 * this.cellRadius;//圆心间距三倍半径
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mCells[i][j].setX(distance * j + cellRadius + offset);
                mCells[i][j].setY(distance * i + cellRadius + offset);
            }
        }
    }

    /**
     * initialize paints
     */
    private void initPaints() {
        outerCirclePaint = new Paint();
        outerCirclePaint.setStyle(Style.FILL);
        outerCirclePaint.setAntiAlias(true);

        middleCirclePaint = new Paint();
        middleCirclePaint.setStyle(Style.FILL);
        middleCirclePaint.setAntiAlias(true);

        innerCirclePaint = new Paint();
        innerCirclePaint.setStyle(Style.FILL);
        innerCirclePaint.setAntiAlias(true);
        innerCirclePaint.setStrokeWidth(5f);

        linePaint = new Paint();
        linePaint.setStyle(Style.FILL);
        linePaint.setAntiAlias(true);
        // TODO: 2019/11/5 1.修改划线为linePaint 2.修改各个默认选择和错误配色 3.配色支持设置和xml属性
    }

    /**
     * draw line not include circle (the line do not show inside the circle)
     *
     * @param preCell
     * @param nextCell
     * @param canvas
     * @param paint
     */
    private void drawLineNotIncludeInnerCircle(Cell preCell, Cell nextCell, Canvas canvas, Paint paint) {
        float distance = LockPatternUtil.getDistanceBetweenTwoPoints(preCell.getX(), preCell.getY(), nextCell.getX(), nextCell.getY());
        float x1 = this.cellInnerRadius / distance * (nextCell.getX() - preCell.getX()) + preCell.getX();
        float y1 = this.cellInnerRadius / distance * (nextCell.getY() - preCell.getY()) + preCell.getY();
        float x2 = (distance - this.cellInnerRadius) / distance * (nextCell.getX() - preCell.getX()) + preCell.getX();
        float y2 = (distance - this.cellInnerRadius) / distance * (nextCell.getY() - preCell.getY()) + preCell.getY();
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    /**
     * draw line follow finger
     * (do not draw line inside the selected cell,
     * but it is only the starting cell not the other's cell)
     *
     * @param preCell
     * @param canvas
     * @param paint
     */
    private void drawLineFollowFinger(Cell preCell, Canvas canvas, Paint paint) {
        float distance = LockPatternUtil.getDistanceBetweenTwoPoints(preCell.getX(), preCell.getY(), movingX, movingY);
        if (distance > this.cellRadius) {
            float x1 = this.cellRadius / distance * (movingX - preCell.getX()) + preCell.getX();
            float y1 = this.cellRadius / distance * (movingY - preCell.getY()) + preCell.getY();
            canvas.drawLine(x1, y1, movingX, movingY, paint);
        }
    }

    /**
     * draw line follow finger
     * (do not draw line inside the selected cell,
     * but it is only the starting cell not the other's cell)
     *
     * @param preCell
     * @param canvas
     * @param paint
     */
    private void drawLineFollowFingerNotIncludeInner(Cell preCell, Canvas canvas, Paint paint) {
        float distance = LockPatternUtil.getDistanceBetweenTwoPoints(preCell.getX(), preCell.getY(), movingX, movingY);
        if (distance > this.cellInnerRadius) {
            float x1 = this.cellInnerRadius / distance * (movingX - preCell.getX()) + preCell.getX();
            float y1 = this.cellInnerRadius / distance * (movingY - preCell.getY()) + preCell.getY();
            canvas.drawLine(x1, y1, movingX, movingY, paint);
        }
    }

    /**
     * handle action down
     *
     * @param ex
     * @param ey
     */
    private void handleActionDown(float ex, float ey) {
        mode = DisplayMode.DOWN;
        if (this.patterListener != null) {
            this.patterListener.onPatternStart();
        }
        Cell cell = checkSelectCell(ex, ey);
        if (cell != null) {
            addSelectedCell(cell);
        }
        // this.setMode(DisplayMode.NORMAL);
        handleStealthMode();
        // handleHapticFeedback();
    }

    public void handleGestureSuccess() {
        for (Cell cell : sCells) {
            cell.setStatus(STATUS.STATE_CHECK_SUCCESS);
        }
        postInvalidate();
    }

    public void handleGestureError() {
        for (Cell cell : sCells) {
            cell.setStatus(STATUS.STATE_CHECK_ERROR);
        }
        mode = DisplayMode.ERROR;
        invalidate();
    }

    /**
     * handle action move
     *
     * @param ex
     * @param ey
     */
    private void handleActionMove(float ex, float ey) {
        mode = DisplayMode.MOVING;
        movingX = ex;
        movingY = ey;
        Cell cell = checkSelectCell(ex, ey);
        if (cell != null) {
            addSelectedCell(cell);
        }
        handleStealthMode();
    }

    /**
     * handle action up
     */
    private void handleActionUp() {
        // mode = DisplayMode.UP;
        if (this.patterListener != null) {
            this.patterListener.onPatternComplete(sCells);
        }
    }

    private void disableGesture() {

    }

    /**
     * check user's touch moving is or not in the area of cells
     *
     * @param x
     * @param y
     * @return
     */
    private Cell checkSelectCell(float x, float y) {
        for (int i = 0; i < mCells.length; i++) {
            for (int j = 0; j < mCells[i].length; j++) {
                Cell cell = mCells[i][j];
                if (LockPatternUtil.checkInRound(cell.x, cell.y, 80, x, y, this.cellRadius / 4)) {
                    return cell;
                }
            }
        }
        return null;
    }

    /**
     * add selected cell
     *
     * @param cell
     */
    private void addSelectedCell(Cell cell) {
        if (!sCells.contains(cell)) {
            cell.setStatus(STATUS.STATE_CHECK);
            sCells.add(cell);
            handleHapticFeedback();
        }
        // setMode(DisplayMode.NORMAL);
    }

    /**
     * handle haptic feedback
     * (if mEnableHapticFeedback true: has haptic else not have haptic)
     */
    private void handleHapticFeedback() {
        if (mEnableHapticFeedback) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    public DisplayMode getMode() {
        return mode;
    }

    /**
     * handle the stealth mode (if true: do not post invalidate; false: post invalidate)
     */
    private void handleStealthMode() {
        Log.d("LockPatternView", "handleStealthMode: ");
        if (!mInStealthMode) {
            this.postInvalidate();
        }
    }

    /**
     * @return Whether the view is in stealth mode.
     */
    public boolean isInStealthMode() {
        return mInStealthMode;
    }

    /**
     * Set whether the view is in stealth mode.  If true, there will be no
     * visible feedback as the user enters the pattern.
     *
     * @param inStealthMode Whether in stealth mode.
     */
    public void setInStealthMode(boolean inStealthMode) {
        mInStealthMode = inStealthMode;
    }

    /**
     * @return Whether the view has tactile feedback enabled.
     */
    public boolean isTactileFeedbackEnabled() {
        return mEnableHapticFeedback;
    }

    /**
     * Set whether the view will use tactile feedback.  If true, there will be
     * tactile feedback as the user enters the pattern.
     *
     * @param tactileFeedbackEnabled Whether tactile feedback is enabled
     */
    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    public void setOnPatternListener(OnPatternListener patternListener) {
        this.patterListener = patternListener;
    }

    /**
     * the display mode of the pattern
     */
    public enum DisplayMode {
        NORMAL,
        //show selected pattern error
        DOWN,
        MOVING,
        //show default pattern (the default pattern is initialize status)
        UP,
        SUCCESS,
        ERROR,
    }

    /**
     * callback interface
     */
    public static interface OnPatternListener {
        public void onPatternStart();

        public void onPatternComplete(List<Cell> cells);
    }

    public class Cell {
        //        //default status
//        public static final int STATE_NORMAL = 0;
//        //checked status
//        public static final int STATE_CHECK = 1;
//        //checked error status
//        public static final int STATE_CHECK_ERROR = 2;
//        public static final int STATE_CHECK_SUCCESS = 3;
        private int x;// the x position of circle's center point
        private int y;// the y position of circle's center point
        private int row;// the cell in which row
        private int column;// the cell in which column
        private int index;// the cell value
        //        private int status = STATE_NORMAL;//default status
        private STATUS status = STATUS.STATE_NORMAL;

        public Cell() {
        }

        public Cell(int x, int y, int row, int column, int index) {
            this.x = x;
            this.y = y;
            this.row = row;
            this.column = column;
            this.index = index;
        }

        public int getX() {
            return this.x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return this.y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getRow() {
            return this.row;
        }

        public int getColumn() {
            return this.column;
        }

        public int getIndex() {
            return this.index;
        }

        public STATUS getStatus() {
            return this.status;
        }

        public void setStatus(STATUS status) {
            this.status = status;
        }
    }

    public enum STATUS {
        STATE_NORMAL,
        STATE_CHECK,
        STATE_CHECK_ERROR,
        STATE_CHECK_SUCCESS
    }
}
