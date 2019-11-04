package cn.naughtyChild.guesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
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
    private boolean isActionMove = false;
    private boolean isActionDown = false;//default action down is false
    private boolean isActionUp = true;//default action up is true
    private int width, height;
    private int cellRadius, cellInnerRadius, middleRadius;
    //in stealth mode (default is false),if set true,the gesture is invisible
    private boolean mInStealthMode = false;
    //haptic feed back (default is true),if set true,it'll shake when gesture drawing.
    private boolean mEnableHapticFeedback = true;
    //set delay time
    private long delayTime = 600L;
    //set offset to the boundary
    private int offset = 10;//left and right spacing
    private DisplayMode mode = DisplayMode.DEFAULT;//default  mode
    private int outerCircleColor, selectColor, errorColor, errorMiddleColor, selectMidPaintColor;
    //draw view used paint
    private Paint outerCirclePaint, selectPaint, errorPaint, errorMiddleCellPint, selectMidPaint;
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

    public int getSelectColor() {
        return selectColor;
    }

    public void setSelectColor(int selectColor) {
        this.selectColor = selectColor;
    }

    public int getSelectMidPaintColor() {
        return selectMidPaintColor;
    }

    public void setSelectMidPaintColor(int selectMidPaintColor) {
        this.selectMidPaintColor = selectMidPaintColor;
    }

    /**
     * initialize
     */
    private void init() {
        outerCircleColor = context.getResources().getColor(R.color.gray);
        selectColor = context.getResources().getColor(R.color.mediumaquamarine);
        selectMidPaintColor = context.getResources().getColor(R.color.darkgreen);
        errorColor = context.getResources().getColor(R.color.indianred);
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
        super.onDraw(canvas);
        switch (mode) {
            case DEFAULT:
                //初始状态绘制
                drawNormalCircle(canvas);
                break;
            case MOVING:
                drawMovingCircle(canvas);
                break;
            case UP:
                drawUPResult(canvas);
                break;
            case DISABLE:
                drawNothing(canvas);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mode == DisplayMode.UP) return true;

        float ex = event.getX();
        float ey = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(ex, ey);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(ex, ey);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp();
                break;
        }
        return true;
    }

    public void restMode() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "重置", Toast.LENGTH_SHORT).show();
                sCells.clear();
                for (Cell[] mCell : mCells) {
                    for (Cell cell : mCell) {
                        cell.status = STATUS.STATE_NORMAL;
                    }
                }
                mode = DisplayMode.DEFAULT;
                postInvalidate();
            }
        }, 2000);
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
                        errorMiddleCellPint.setStyle(Style.FILL);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.errorMiddleCellPint);
                        errorPaint.setStyle(Style.FILL);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.errorPaint);
                        break;
                    case STATE_CHECK_SUCCESS:
                        // TODO: 2019/11/1
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
                        selectMidPaint.setStyle(Style.FILL);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.selectMidPaint);
                        selectPaint.setStyle(Style.FILL);
                        canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.selectPaint);
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
                    drawLineNotIncludeInnerCircle(tempCell, cell, canvas, selectPaint);
                } else if (cell.getStatus() == STATUS.STATE_CHECK_ERROR) {
                    drawLineNotIncludeInnerCircle(tempCell, cell, canvas, errorPaint);
                }
                tempCell = cell;
            }
            if (isActionMove && !isActionUp) {
                drawLineFollowFingerNotIncludeInner(tempCell, canvas, selectPaint);
            }
        }
    }


    private void drawNothing(Canvas canvas) {
    }

    private void drawNormalCircle(Canvas canvas) {
        for (int i = 0; i < mCells.length; i++) {
            for (int j = 0; j < mCells[i].length; j++) {
                //正常时候，绘制外圆
                canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellRadius, this.outerCirclePaint);
            }
        }
    }


    /**
     * draw the view to canvas
     *
     * @param canvas
     */
    private void drawToCanvas(Canvas canvas) {
     /*   for (int i = 0; i < mCells.length; i++) {
            for (int j = 0; j < mCells[i].length; j++) {
                //选中时候，绘制中间园
                if (mCells[i][j].getStatus() == Cell.STATE_CHECK) {
                    selectMidPaint.setStyle(Style.FILL);
                    canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.selectMidPaint);
                    selectPaint.setStyle(Style.FILL);
                    canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.selectPaint);
                } else if (mCells[i][j].getStatus() == Cell.STATE_NORMAL) {
                    //正常时候，绘制外圆
                    canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellRadius, this.outerCirclePaint);
                } else if (mCells[i][j].getStatus() == Cell.STATE_CHECK_ERROR) {
                    //判定错误时候
                    errorMiddleCellPint.setStyle(Style.FILL);
                    canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.middleRadius, this.errorMiddleCellPint);
                    errorPaint.setStyle(Style.FILL);
                    canvas.drawCircle(mCells[i][j].getX(), mCells[i][j].getY(), this.cellInnerRadius, this.errorPaint);
                }
            }
        }
        if (sCells.size() > 0) {
            //temporary cell: at the beginning the cell is the first of sCells
            Cell tempCell = sCells.get(0);
            for (int i = 1; i < sCells.size(); i++) {
                Cell cell = sCells.get(i);
                if (cell.getStatus() == Cell.STATE_CHECK) {
                    drawLineNotIncludeInnerCircle(tempCell, cell, canvas, selectPaint);
                } else if (cell.getStatus() == Cell.STATE_CHECK_ERROR) {
                    drawLineNotIncludeInnerCircle(tempCell, cell, canvas, errorPaint);
                }
                tempCell = cell;
            }
            if (isActionMove && !isActionUp) {
                drawLineFollowFingerNotIncludeInner(tempCell, canvas, selectPaint);
            }
        }*/
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
        outerCirclePaint.setColor(outerCircleColor);
        outerCirclePaint.setStyle(Style.FILL);
        outerCirclePaint.setAntiAlias(true);
        selectPaint = new Paint();
        selectPaint.setColor(selectColor);
        selectPaint.setStrokeWidth(5.0f);
        selectPaint.setStyle(Style.FILL);
        selectPaint.setAntiAlias(true);
        errorPaint = new Paint();
        errorPaint.setColor(errorColor);
        errorPaint.setStrokeWidth(5.0f);
        errorPaint.setStyle(Style.FILL);
        errorPaint.setAntiAlias(true);
        errorMiddleCellPint = new Paint();
        errorMiddleCellPint.setColor(errorMiddleColor);
        errorMiddleCellPint.setAntiAlias(true);
        selectMidPaint = new Paint();
        selectMidPaint.setColor(outerCircleColor);
        selectMidPaint.setAntiAlias(true);
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
        isActionMove = false;
        isActionDown = true;
        isActionUp = false;
        if (this.patterListener != null) {
            this.patterListener.onPatternStart();
        }
        Cell cell = checkSelectCell(ex, ey);
        if (cell != null) {
            addSelectedCell(cell);

        }
        // this.setMode(DisplayMode.DEFAULT);
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
        postInvalidate();
    }

    /**
     * handle action move
     *
     * @param ex
     * @param ey
     */
    private void handleActionMove(float ex, float ey) {
        mode = DisplayMode.MOVING;
        isActionMove = true;
        movingX = ex;
        movingY = ey;
        Cell cell = checkSelectCell(ex, ey);
        if (cell != null) {
            addSelectedCell(cell);
            // handleHapticFeedback();
        }
        //this.setMode(DisplayMode.NORMAL);
        handleStealthMode();

    }

    /**
     * handle action up
     */
    private void handleActionUp() {
        mode = DisplayMode.UP;
        isActionMove = false;
        isActionUp = true;
        isActionDown = false;
        // this.setMode(DisplayMode.NORMAL);
        handleHapticFeedback();
        handleStealthMode();
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

    public void setMode(DisplayMode mode) {
        this.mode = mode;
//        switch (mode) {
//            case DEFAULT:
//                for (Cell cell : sCells) {
//                    cell.setStatus(STATUS.STATE_NORMAL);
//                }
//                sCells.clear();
//                break;
//            case NORMAL:
//                break;
//            case RIGHT:
//                break;
//            case DISABLE:
//                break;
//            case ERROR:
//                for (Cell cell : sCells) {
//                    cell.setStatus(STATUS.STATE_CHECK_ERROR);
//                }
//                break;
//        }
//        this.handleStealthMode();
    }

    /**
     * set pattern
     *
     * @param mode (details see the DisplayMode)
     */
    @Deprecated
    public void setPattern(DisplayMode mode) {
        switch (mode) {
            case DEFAULT:
                for (Cell cell : sCells) {
                    cell.setStatus(STATUS.STATE_NORMAL);
                }
                sCells.clear();
                break;
            case NORMAL:
                break;
            case ERROR:
                for (Cell cell : sCells) {
                    cell.setStatus(STATUS.STATE_CHECK_ERROR);
                }
                break;
        }
        this.handleStealthMode();
    }

    /**
     * handle the stealth mode (if true: do not post invalidate; false: post invalidate)
     */
    private void handleStealthMode() {
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
        DEFAULT,
        //show selected pattern normal
        NORMAL,
        //show selected pattern error
        MOVING,
        //show default pattern (the default pattern is initialize status)
        ERROR,
        //show selected pattern right
        UP,
        //show selected pattern error
        DISABLE;
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
