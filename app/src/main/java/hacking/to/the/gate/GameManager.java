package hacking.to.the.gate;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import java.util.Random;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Manager class that manages the game logic, holds all underlying data objects, and renders the canvas.
 *
 * TODO: Should probably seperate the call {@link #tick()} and {@link #draw(android.graphics.Canvas)} into different threads.
 * TODO: Rendering should be using a defensive copy of the underlying data objects to avoid synchronization and performance problem.
 *
 * Created by yihuaqi on 2015/8/17.
 */
public class GameManager {
    private static GameManager instance;
    private GameManager(){
    };
    public static GameManager getInstance(){
        if(instance==null){
            instance = new GameManager();
        }
        return instance;
    }

    /**
     * Jet that controled by player.
     */
    private Jet mSelfJet;
    /**
     * List of enemy Jets.
     */
    private List<Jet> mEnemyJets;

    /*
        list of power ups
     */
    private List<PowerUp> mPowerUps;
    private float mScreenWidth;
    private float mScreenHeight;
    private Rect mScreenRect;

    /**
     * Init the {@link hacking.to.the.gate.GameManager} with the dimension of the {@link hacking.to.the.gate.GameView}.
     * Create SelfJet and EnemyJets.
     *
     * TODO: Creating SelfJet and EnemyJets should be in seperate methods.
     *
     * @param screenWidht
     * @param screenHeight
     */
    public void init(float screenWidht, float screenHeight){
        mScreenWidth = screenWidht;
        mScreenHeight = screenHeight;
        mScreenRect = new Rect(0,0,(int)mScreenWidth,(int)mScreenHeight);
        Paint p = new Paint();
        Paint p2 = new Paint();
        p2.setColor(Color.RED);
        p.setColor(Color.WHITE);
        mFPSPaint = new Paint();
        mFPSPaint.setColor(Color.WHITE);

        mSelfJet = new Jet(mScreenWidth/2,mScreenHeight-50,50,p,true);
        mSelfJet.setGunType(Gun.GUN_TYPE_DEFAULT);

        mEnemyJets = new LinkedList<>();
        for(int i =0; i<5;i++){
            Jet enemyJet= new Jet((i+1)*mScreenWidth/6,0, 50, p2,false);
            enemyJet.setGunType(Gun.GUN_TYPE_SELF_TARGETING);
            mEnemyJets.add(enemyJet);

        }

        mPowerUps = new LinkedList<>();
    }

    /**
     * Set the destination of the SelfJet.
     * @param event MotionEvent of user touch.
     */
    public void setSelfJetDest(MotionEvent event){
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mSelfJet.setDestination(event.getX(),event.getY(),true);
                break;
            case MotionEvent.ACTION_UP:
                mSelfJet.setDestination(event.getX(),event.getY(),false);
                break;
        }
    }

    /**
    * Tick to the next move.
     */
    public void tick(){

        if(!mSelfJet.isDead()) {
            mSelfJet.tick();
            for(Iterator<PowerUp> i = mPowerUps.iterator();i.hasNext();) {
                PowerUp p = i.next();
                if(!mSelfJet.isDead()&&p.isVisible()&&CollisionEngine.detectCollision(mSelfJet,p)){
                    mSelfJet.doCollision(p);
                }
            }
        }


        for(Jet jet:mEnemyJets){
            for(Iterator<Bullet> it = jet.getBullets().iterator(); it.hasNext();){
                Bullet b = it.next();
               if(!mSelfJet.isDead()&&CollisionEngine.detectCollision(mSelfJet,b)) {
                   Log.d("collision","hit by enemy's bullet");
                   mSelfJet.doCollision(b);
               }
            }

            for(Iterator<Bullet> it = mSelfJet.getBullets().iterator(); it.hasNext();){
                Bullet b = it.next();
                if(!jet.isDead()&&CollisionEngine.detectCollision(jet,b)) {
                    Log.d("collision","hit by player's bullet");
                    jet.doCollision(b);
                }
            }

            jet.tick();

        }

        for(PowerUp p :mPowerUps){
            p.tick();
        }

        if(shouldGeneratePowerUps()){
            Random rand = new Random();
            int value = rand.nextInt(50)+1;
            int randomX = 20*value;
            int randomY = 10*value;
            generatePowerups(true,randomX,randomY);

        }
        recycle();
    }

    /**
     * generate a powerup
     * @param isStatic identify if the powerup is static
     * @param posX x-position of the center of the powerup
     * @param posY y-position of the center of the powerup
     */
    public void generatePowerups(boolean isStatic,int posX,int posY){
        PowerUp powerUp;
        Paint powerUpPaint = new Paint();
        powerUpPaint.setColor(Color.GREEN);
        Position pos = new Position(posX,posY);
        if(isStatic){
            powerUp = new PowerUp(isStatic,pos,0,0, powerUpPaint,23);

        }
        else {
            powerUp = new PowerUp(isStatic, pos, 1, 4, powerUpPaint, 23);
        }
        mPowerUps.add(powerUp);

    }

    /**
     *
     * @return true if it's time to generate powerups
     */
    public boolean shouldGeneratePowerUps(){
        return mPowerUps.size()<4&& mSelfJet.getHealth()<80;
    }
    /**
     * Render to canvas
     * @param canvas
     */
    public void draw(Canvas canvas){
        canvas.drawColor(Color.BLACK);
        if(!mSelfJet.isDead()) {
            mSelfJet.draw(canvas);
        }

        for(Jet jet:mEnemyJets){
            jet.draw(canvas);
        }
        for(PowerUp p:mPowerUps){
            if(p.isVisible()){
                p.draw(canvas);
            }
        }
    }

    public Rect getScreenRect(){
        return mScreenRect;

    }

    public void recycle(){
        if(mSelfJet.shouldRecycle()){

        } else {
            mSelfJet.recycle();
        }

        for(Iterator<Jet> it = mEnemyJets.iterator(); it.hasNext();){
            Jet jet = it.next();
            if(jet.shouldRecycle()){
                it.remove();
                Random rand = new Random();
                int randomX = rand.nextInt(50)+100;
                int randonY = rand.nextInt(50)+200;
                generatePowerups(false,randomX,randonY);
            } else {
                jet.recycle();
            }
        }
        for(Iterator<PowerUp> i = mPowerUps.iterator();i.hasNext();){
            PowerUp p = i.next();
            if(!p.isVisible()){
                i.remove();
                Log.d("PowerUp","removed");
            }
        }
    }

    private long lastTimestamp = 0;
    private Paint mFPSPaint;

    /**
     * This will measure and display the FPS on the left top corner.
     * @param c
     */
    public void measureFrameRate(Canvas c) {
        if(lastTimestamp != 0) {
            double frameRate = 1000/(System.currentTimeMillis() - lastTimestamp);
            c.drawText("FPS: "+frameRate,10,10,mFPSPaint);
        }
        lastTimestamp = System.currentTimeMillis();
    }

    public Position getSelfJetPosition(){
        return mSelfJet.getSelfPosition();
    }
}
