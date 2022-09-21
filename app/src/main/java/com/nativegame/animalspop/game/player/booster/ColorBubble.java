package com.nativegame.animalspop.game.player.booster;

import com.nativegame.animalspop.R;
import com.nativegame.animalspop.game.MyGameEvent;
import com.nativegame.animalspop.game.bubble.Bubble;
import com.nativegame.animalspop.game.bubble.BubbleColor;
import com.nativegame.animalspop.game.bubble.BubbleSystem;
import com.nativegame.animalspop.game.player.dot.DotSystem;
import com.nativegame.animalspop.game.player.PlayerBubble;
import com.nativegame.animalspop.sound.MySoundEvent;
import com.nativegame.engine.GameEngine;
import com.nativegame.engine.particles.ParticleSystem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Oscar Liang on 2022/09/18
 */

public class ColorBubble extends PlayerBubble {

    private final ParticleSystem mTrailParticleSystem;
    private final ParticleSystem mSparkleParticleSystem;
    private boolean mConsume;

    public ColorBubble(BubbleSystem bubbleSystem, GameEngine gameEngine) {
        super(bubbleSystem, gameEngine, R.drawable.color_bubble);
        mTrailParticleSystem = new ParticleSystem(gameEngine, R.drawable.sparkle, 50)
                .setDuration(300)
                .setEmissionRate(ParticleSystem.RATE_HIGH)
                .setAccelerationX(-2, 2)
                .setInitialRotation(0, 360)
                .setRotationSpeed(-720, 720)
                .setAlpha(255, 0)
                .setScale(2, 4);
        mSparkleParticleSystem = new ParticleSystem(gameEngine, R.drawable.white_particle, 10)
                .setDuration(400)
                .setSpeedX(-1000, 1000)
                .setSpeedY(-1000, 1000)
                .setInitialRotation(0, 360)
                .setRotationSpeed(-720, 720)
                .setAlpha(255, 0)
                .setScale(1, 0.5f);
        // Init dot color
        mDotSystem.setDotBitmap(R.drawable.dot_white);
    }

    public void init(GameEngine gameEngine) {
        mX = mStartX - mWidth / 2f;
        mY = mStartY - mHeight / 2f;
        mConsume = false;
        addToGameEngine(gameEngine, 2);
        mSparkleParticleSystem.emit();   // Start emitting
    }

    @Override
    protected DotSystem getDotSystem(GameEngine gameEngine) {
        return new DotSystem(this, gameEngine);
    }

    @Override
    public void startGame(GameEngine gameEngine) {
    }

    @Override
    public void addToGameEngine(GameEngine gameEngine, int layer) {
        super.addToGameEngine(gameEngine, layer);
        mTrailParticleSystem.addToGameEngine(gameEngine, layer - 1);
        mSparkleParticleSystem.addToGameEngine(gameEngine, layer - 1);
        gameEngine.onGameEvent(MyGameEvent.BOOSTER_ADDED);
        gameEngine.mSoundManager.playSound(MySoundEvent.ADD_BOOSTER);
    }

    @Override
    public void removeFromGameEngine(GameEngine gameEngine) {
        super.removeFromGameEngine(gameEngine);
        mTrailParticleSystem.removeFromGameEngine(gameEngine);
        mSparkleParticleSystem.removeFromGameEngine(gameEngine);
        if (mConsume) {
            // Update player state and notify the booster manager
            gameEngine.onGameEvent(MyGameEvent.BOOSTER_CONSUMED);
        } else {
            // Update player state
            gameEngine.onGameEvent(MyGameEvent.BOOSTER_REMOVED);
        }
    }

    @Override
    public void onUpdate(long elapsedMillis, GameEngine gameEngine) {
        super.onUpdate(elapsedMillis, gameEngine);
        mTrailParticleSystem.setEmissionPosition(mX + mWidth / 2f, mY + mHeight / 2f);
        mSparkleParticleSystem.setEmissionPosition(mX + mWidth / 2f, mY + mHeight / 2f);
    }

    @Override
    protected void onBubbleShoot(GameEngine gameEngine) {
        mTrailParticleSystem.emit();   // Start emitting
        gameEngine.onGameEvent(MyGameEvent.BOOSTER_SHOT);
        gameEngine.mSoundManager.playSound(MySoundEvent.BUBBLE_SHOOT);
    }

    @Override
    protected void onBubbleSwitch(GameEngine gameEngine) {
        // We do not switch booster here
    }

    @Override
    protected void onBubbleHit(GameEngine gameEngine, Bubble bubble) {
        if (bubble.mBubbleColor != BubbleColor.BLANK
                && mY >= bubble.mY
                && !mConsume) {
            // Get the new added bubble
            Bubble newBubble = mBubbleSystem.getBubble(this, bubble);
            // Pop all the bubble in edges
            for (Bubble b : newBubble.mEdges) {
                if (b.mBubbleColor != BubbleColor.BLANK) {
                    bfs(gameEngine, b, b.mBubbleColor);
                }
            }
            gameEngine.mSoundManager.playSound(MySoundEvent.BUBBLE_HIT);
            reset(gameEngine);
        }
    }

    private void bfs(GameEngine gameEngine, Bubble root, BubbleColor color) {
        ArrayList<Bubble> deleteList = new ArrayList<>();
        Queue<Bubble> queue = new LinkedList<>();
        root.mDepth = 0;
        queue.offer(root);

        while (!queue.isEmpty()) {
            Bubble currentBubble = queue.poll();
            deleteList.add(currentBubble);
            for (Bubble b : currentBubble.mEdges) {
                // Unvisited bubble
                if (b.mDepth == -1 && b.mBubbleColor == color) {
                    b.mDepth = currentBubble.mDepth + 1;
                    queue.offer(b);
                }
            }
        }

        // Update bubble after bfs
        for (Bubble b : deleteList) {
            b.popBubble(gameEngine);
        }
        deleteList.clear();
    }

    @Override
    protected void onBubbleReset(GameEngine gameEngine) {
        // Pop floater and shift
        mBubbleSystem.popFloater();
        mBubbleSystem.shiftBubble();
        // Stop emitting
        mTrailParticleSystem.stopEmit();
        mSparkleParticleSystem.stopEmit();
        mConsume = true;
        removeFromGameEngine(gameEngine);
    }

}
