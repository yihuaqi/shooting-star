package hacking.to.the.gate;

import android.graphics.Canvas;

/**
 * Created by yihuaqi on 2015/8/15.
 */
public class GameLoopThread extends Thread{
    private GameView view;
    private boolean running = false;
    public GameLoopThread(GameView view){
        this.view = view;
    }
    public void setRunning(boolean run){
        running = run;
    }

    @Override
    public void run() {
        super.run();
        while(running) {
            Canvas c = null;
            try{
                c = view.getHolder().lockCanvas();
                synchronized (view.getHolder()){
                    view.onDraw(c);
                }
            }finally {
                if(c!= null){
                    view.getHolder().unlockCanvasAndPost(c);
                }
            }
        }
    }
}