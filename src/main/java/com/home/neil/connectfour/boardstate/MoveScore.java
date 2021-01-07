package com.home.neil.connectfour.boardstate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.home.neil.connectfour.managers.appmanager.ApplicationPrecompilerSettings;


public class MoveScore {


	public static final String CLASS_NAME = MoveScore.class.getName();
	public static final String PACKAGE_NAME = CLASS_NAME.substring(0,
			CLASS_NAME.lastIndexOf("."));
	public static Logger sLogger = LogManager.getLogger(PACKAGE_NAME);
	
	public BoardState mBoardState;
	
	private byte mMoveScore = 0;

	public MoveScore(BoardState pBoardState) {
		super();
		if (ApplicationPrecompilerSettings.TRACELOGACTIVE) {
			sLogger.trace("Entering");
		}
		mBoardState = pBoardState;
		if (ApplicationPrecompilerSettings.TRACELOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}

	public byte getMoveScore() {
		if (ApplicationPrecompilerSettings.TRACELOGACTIVE) {
			sLogger.trace("Entering");
		}
		if (ApplicationPrecompilerSettings.TRACELOGACTIVE) {
			sLogger.trace("Exiting");
		}
		return mMoveScore;
	}


	public void setMoveScore(byte pMoveScore) {
		if (ApplicationPrecompilerSettings.TRACELOGACTIVE) {
			sLogger.trace("Entering");
		}
		mMoveScore = pMoveScore;
		if (ApplicationPrecompilerSettings.TRACELOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}
}
