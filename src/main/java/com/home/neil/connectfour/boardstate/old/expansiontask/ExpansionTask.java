package com.home.neil.connectfour.boardstate.old.expansiontask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;

import javax.management.openmbean.OpenDataException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.home.neil.connectfour.boardstate.old.InvalidMoveException;
import com.home.neil.connectfour.boardstate.old.OldBoardState;
import com.home.neil.connectfour.boardstate.old.OldBoardState.GameState;
import com.home.neil.connectfour.boardstate.old.locks.BoardStateLocks;
import com.home.neil.connectfour.knowledgebase.KnowledgeBaseFilePool;
import com.home.neil.connectfour.knowledgebase.exception.KnowledgeBaseException;
import com.home.neil.connectfour.managers.appmanager.ApplicationPrecompilerSettings;
import com.home.neil.connectfour.performancemetrics.ThreadPerformanceMetricsMBean;



public class ExpansionTask {
	public static final String SIMPLE_CLASS_NAME = ExpansionTask.class.getSimpleName();
	public static final String CLASS_NAME = ExpansionTask.class.getName();
	public static final String PACKAGE_NAME = CLASS_NAME.substring(0, CLASS_NAME.lastIndexOf("."));
	public static Logger sLogger = LogManager.getLogger(PACKAGE_NAME);

	private OldBoardState mInitialBoardStateToExpand = null;

	private volatile OldBoardState mBoardStateLock= null;

	private ArrayList<OldBoardState> mSubBoardStates = null;

	private boolean mTransactionSuccessful = false;

	private String mLogContext = null;

	private static int sTaskNumber = 0;

	private long mThreadStartTime = 0;
	private long mThreadEndTime = 0;

	public String getName() {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
		return mLogContext;
	}

	private static synchronized void incTaskNumber() {
		sTaskNumber++;
	}

	public void renameTask(String pLogContext) {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}
		incTaskNumber();

		if (pLogContext == null || pLogContext.trim().isEmpty()) {
			mLogContext = SIMPLE_CLASS_NAME + "." + sTaskNumber;
			ThreadContext.put("LogContext", mLogContext);
		} else {
			mLogContext = pLogContext;
			ThreadContext.put("LogContext", mLogContext);
		}

		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}

	public ExpansionTask(OldBoardState pInitialBoardStateToExpand, String pLogContext) {
		super();
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}

		renameTask(pLogContext);

		mInitialBoardStateToExpand = pInitialBoardStateToExpand;

		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}


	public ArrayList<OldBoardState> getSubBoardStates() {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
		return mSubBoardStates;
	}

	public void setBoardStateLock(OldBoardState pBoardState) {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}
		mBoardStateLock = pBoardState;
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}

	public boolean executeTask() {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}


		if (ApplicationPrecompilerSettings.TASKMETRICSACTIVE) {
			mThreadStartTime = new GregorianCalendar().getTimeInMillis();
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Task: " + mLogContext + " is starting at " + mThreadStartTime);
			}
		}
		

		try {
			expandAndRescoreNode();
		} catch (Exception eE) {
			sLogger.error("Unanticipated Exception occurred during Task Execution!");
			sLogger.error("Exception Message: " + eE.getMessage());

			StringWriter lSW = new StringWriter();
			PrintWriter lPW = new PrintWriter(lSW);
			eE.printStackTrace(lPW);
			lSW.toString(); // stack trace as a string
			sLogger.error("StackTrace: " + lSW);

			mTransactionSuccessful = false;
		}

		if (ApplicationPrecompilerSettings.TASKMETRICSACTIVE) {
			mThreadEndTime = new GregorianCalendar().getTimeInMillis();
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Thread: " + mLogContext + " is ending at " + mThreadEndTime);
			}
			long lDuration = mThreadEndTime - mThreadStartTime;
		
			try {
				ThreadPerformanceMetricsMBean lThreadPerformanceMetricsMBean = ThreadPerformanceMetricsMBean.getInstance();
				lThreadPerformanceMetricsMBean.updateThreadStatistics(SIMPLE_CLASS_NAME, "run()", mThreadStartTime, lDuration, mTransactionSuccessful);
			} catch (OpenDataException eODE) {
				sLogger.error("Open Data Exception occurred during Task Execution!");
				sLogger.error("Exception Message: " + eODE.getMessage());
	
				StringWriter lSW = new StringWriter();
				PrintWriter lPW = new PrintWriter(lSW);
				eODE.printStackTrace(lPW);
				lSW.toString(); // stack trace as a string
				sLogger.error("StackTrace: " + lSW);
			} catch (Exception eE) {
				sLogger.error("Exception Message: " + eE.getMessage());
	
				StringWriter lSW = new StringWriter();
				PrintWriter lPW = new PrintWriter(lSW);
				eE.printStackTrace(lPW);
				lSW.toString(); // stack trace as a string
				sLogger.error("StackTrace: " + lSW);
			}
		}
	
			
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
		
		return mTransactionSuccessful;
	}

	private void expandAndRescoreNode() {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}


		OldBoardState lCurrentBoardState = mInitialBoardStateToExpand;
		OldBoardState lParentBoardState = mInitialBoardStateToExpand.getParentBoardState();
		OldBoardState lBoardStateToRescore = null;
		boolean lRescore = false;
		byte lRescoreValue = 0;
		ArrayList <OldBoardState> lSubBoardStates = null;
		
		sLogger.debug("Starting with the Current BoardState: " + mInitialBoardStateToExpand.getFileIndexString());
				
		do {
			
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Reserving the Current BoardState: " + lCurrentBoardState.getFileIndexString());
			}
			
			boolean lStateReserved = reserveBoardState(lCurrentBoardState);
			if (!lStateReserved) {
				sLogger.error("COULD NOT GET THE BOARDSTATE RESERVED!");
				mTransactionSuccessful = false;
				if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
					sLogger.trace("Exiting");
				}
				return;
			}
			
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Current BoardState is reserved: " + lCurrentBoardState.getFileIndexString());
				sLogger.debug("Expanding SubBoard States: " + lCurrentBoardState.getFileIndexString());
			}
	
			try {
				lSubBoardStates = expandSubBoardStates(lCurrentBoardState);
			} catch (KnowledgeBaseException eKBE) {
				sLogger.error("KNOWLEDGE BASE EXCEPTION occurred!");
	
				StringWriter lSW = new StringWriter();
				PrintWriter lPW = new PrintWriter(lSW);
				eKBE.printStackTrace(lPW);
				lSW.toString(); // stack trace as a string
				sLogger.error("StackTrace: " + lSW);
	
				mTransactionSuccessful = false;
				if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
					sLogger.trace("Exiting");
				}
				return;
			} catch (ConfigurationException eCE) {
				sLogger.error("CONFIGURATION EXCEPTION occurred!" + OldBoardState.Move.SELF_SLOT0.getMoveIntValue());
	
				StringWriter lSW = new StringWriter();
				PrintWriter lPW = new PrintWriter(lSW);
				eCE.printStackTrace(lPW);
				lSW.toString(); // stack trace as a string
				sLogger.error("StackTrace: " + lSW);
	
				mTransactionSuccessful = false;
				if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
					sLogger.trace("Exiting");
				}
				return;
			}		
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Current BoardState SubBoard States Expanded: " + lCurrentBoardState.getFileIndexString());
			}
			
			//record the subboardstates of initial node
			if (mSubBoardStates == null) {
				mSubBoardStates = lSubBoardStates;
			}
			
	
			if (lRescore) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Rescore required, setting Move Score and writing to knowledge base.");
				}
				lBoardStateToRescore.getMoveScore().setMoveScore(lRescoreValue);
				try {
					lCurrentBoardState.writeMoveScoreToKnowledge(mLogContext);
				} catch (KnowledgeBaseException eKBE) {
					sLogger.error("KNOWLEDGE BASE EXCEPTION occurred!");
	
					StringWriter lSW = new StringWriter();
					PrintWriter lPW = new PrintWriter(lSW);
					eKBE.printStackTrace(lPW);
					lSW.toString(); // stack trace as a string
					sLogger.error("StackTrace: " + lSW);
	
					mTransactionSuccessful = false;
					if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
						sLogger.trace("Exiting");
					}
					return;
				} catch (ConfigurationException eCE) {
					sLogger.error("CONFIGURATION EXCEPTION occurred!");
	
					StringWriter lSW = new StringWriter();
					PrintWriter lPW = new PrintWriter(lSW);
					eCE.printStackTrace(lPW);
					lSW.toString(); // stack trace as a string
					sLogger.error("StackTrace: " + lSW);
	
					mTransactionSuccessful = false;
					if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
						sLogger.trace("Exiting");
					}
					return;
				} catch (InvalidMoveException eIME) {
					sLogger.error("InvalidMove EXCEPTION occurred!");
	
					StringWriter lSW = new StringWriter();
					PrintWriter lPW = new PrintWriter(lSW);
					eIME.printStackTrace(lPW);
					lSW.toString(); // stack trace as a string
					sLogger.error("StackTrace: " + lSW);
	
					mTransactionSuccessful = false;
					if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
						sLogger.trace("Exiting");
					}
					return;
				}
			}
			
			if (lSubBoardStates == null || lSubBoardStates.isEmpty()) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("CurrentNode could not be expanded: Moves: " + lCurrentBoardState.getFileIndexString() + " Score: "
						+ lCurrentBoardState.getMoveScore().getMoveScore());
				}
				
			} else {
				byte lCurrentNodeScore = rescoreNode(lCurrentBoardState, lSubBoardStates);
	
				if (lParentBoardState == null) {
					// Parent state is already locked.
					if (sLogger.isDebugEnabled()) {
						sLogger.debug("Rescoring the root node: " + lCurrentBoardState.getFileIndexString() + " to " + lCurrentNodeScore);
					}
					lCurrentBoardState.getMoveScore().setMoveScore(lCurrentNodeScore);
					try {
						lCurrentBoardState.writeMoveScoreToKnowledge(mLogContext);
					} catch (ConfigurationException eCE) {
						sLogger.error("CONFIGURATION EXCEPTION occurred!");
	
						StringWriter lSW = new StringWriter();
						PrintWriter lPW = new PrintWriter(lSW);
						eCE.printStackTrace(lPW);
						lSW.toString(); // stack trace as a string
						sLogger.error("StackTrace: " + lSW);
	
						mTransactionSuccessful = false;
						if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
							sLogger.trace("Exiting");
						}
						return;
					} catch (KnowledgeBaseException eKBE) {
						sLogger.error("KNOWLEDGE BASE EXCEPTION occurred!");
	
						StringWriter lSW = new StringWriter();
						PrintWriter lPW = new PrintWriter(lSW);
						eKBE.printStackTrace(lPW);
						lSW.toString(); // stack trace as a string
						sLogger.error("StackTrace: " + lSW);
	
						mTransactionSuccessful = false;
						if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
							sLogger.trace("Exiting");
						}
						return;
					} catch (InvalidMoveException eIME) {
						sLogger.error("InvalidMove EXCEPTION occurred!");
	
						StringWriter lSW = new StringWriter();
						PrintWriter lPW = new PrintWriter(lSW);
						eIME.printStackTrace(lPW);
						lSW.toString(); // stack trace as a string
						sLogger.error("StackTrace: " + lSW);
	
						mTransactionSuccessful = false;
						if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
							sLogger.trace("Exiting");
						}
						return;
					} 
				} else {
					if (sLogger.isDebugEnabled()) {
						sLogger.debug("Rescoring the current node: " + lCurrentBoardState.getFileIndexString() + " to " + lCurrentNodeScore);
					}

					if (lBoardStateToRescore != null) {
						if (sLogger.isDebugEnabled()) {
							sLogger.debug("Releasing the node to rescore (if any): " + lCurrentBoardState.getFileIndexString());
						}
						releaseBoardState(lBoardStateToRescore);
						if (sLogger.isDebugEnabled()) {
							sLogger.debug("Current Node Released: " + lCurrentBoardState.getFileIndexString());
						}
					}
			
					
					lBoardStateToRescore = lCurrentBoardState;
					lCurrentBoardState = lParentBoardState;
					lParentBoardState = lCurrentBoardState.getParentBoardState();
					lRescoreValue = lCurrentNodeScore;
					lRescore = true;
					
				}
			}
			
		
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Current Node Released: " + lCurrentBoardState.getFileIndexString());
			}
		} while (lParentBoardState != null && lSubBoardStates != null && !lSubBoardStates.isEmpty());
		
		
		if (sLogger.isDebugEnabled()) {
			sLogger.debug("Releasing the current node: " + lCurrentBoardState.getFileIndexString());
		}
		releaseBoardState(lCurrentBoardState);
		if (sLogger.isDebugEnabled()) {
			sLogger.debug("Current Node Released: " + lCurrentBoardState.getFileIndexString());
		}

		// Should not have to release
		if (lBoardStateToRescore != null) {
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Releasing the node to rescore (if any): " + lCurrentBoardState.getFileIndexString());
			}
			releaseBoardState(lBoardStateToRescore);
			if (sLogger.isDebugEnabled()) {
				sLogger.debug("Current Node Released: " + lCurrentBoardState.getFileIndexString());
			}
		}
		
		mTransactionSuccessful = true;

		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}

	protected boolean reserveBoardState(OldBoardState pBoardStateToLock) {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}

		BoardStateLocks lBoardStateLocks = BoardStateLocks.getInstance();

		boolean lGotLock = lBoardStateLocks.reserveBoardState(pBoardStateToLock, this);

		if (!lGotLock) {
			while (mBoardStateLock == null) {
//				try {
//					Thread.sleep(10000);
//				} catch (InterruptedException eIE) {
//					// sLogger.trace("Unanticipated Interrupt exception occurred!");
//					//
//					// StringWriter lSW = new StringWriter();
//					// PrintWriter lPW = new PrintWriter(lSW);
//					// eIE.printStackTrace(lPW);
//					// lSW.toString(); // stack trace as a string
//					// sLogger.trace("StackTrace: " + lSW);
//
//				}
			}
		} else {
			mBoardStateLock = pBoardStateToLock;
		}

		if (mBoardStateLock == null) {
			sLogger.error("COULD NOT GET THE BOARDSTATE RESERVED!");
			if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
				sLogger.trace("Exiting");
			}
			mBoardStateLock = null;
			return false;
		}

		mBoardStateLock = null;

		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
		return true;
	}

	protected void releaseBoardState(OldBoardState pBoardStateToUnlock) {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}

		BoardStateLocks lBoardStateLocks = BoardStateLocks.getInstance();

		lBoardStateLocks.releaseBoardState(pBoardStateToUnlock);

		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}
	}

	public byte rescoreNode(OldBoardState pBoardState, ArrayList<OldBoardState> pSubBoardStates) {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}

		// highest or lowest scoring SubBoardState
		// depth of the current object
		int lVirtualDepth = pBoardState.getFileIndexString().length();

		if (sLogger.isDebugEnabled()) {
			sLogger.debug("Move Depth: " + lVirtualDepth);
		}
		int lDepthModulus = lVirtualDepth % 2;

		if (lDepthModulus == 1) { // current move is opponent move, reorder sub
									// moves by highest score
			Collections.sort(pSubBoardStates, new Comparator<OldBoardState>() {
				public int compare(OldBoardState p1, OldBoardState p2) {
					byte lP1MoveScore = p1.getMoveScore().getMoveScore();
					byte lP2MoveScore = p2.getMoveScore().getMoveScore();
					return (lP2MoveScore > lP1MoveScore) ? 1 : -1;
				}
			});
		} else { // current move is a self move, reorder sub moves by lowest
					// score
			Collections.sort(pSubBoardStates, new Comparator<OldBoardState>() {
				public int compare(OldBoardState p1, OldBoardState p2) {
					byte lP1MoveScore = p1.getMoveScore().getMoveScore();
					byte lP2MoveScore = p2.getMoveScore().getMoveScore();
					return (lP2MoveScore < lP1MoveScore) ? 1 : -1;
				}
			});
		}

		byte lScore = pSubBoardStates.get(0).getMoveScore().getMoveScore();

		if (sLogger.isDebugEnabled()) {
			sLogger.debug("CurrentNode: Move: " + pBoardState.getFileIndexString() + " Score: " + pSubBoardStates.get(0).getMoveScore().getMoveScore());
		}
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Exiting");
		}

		return lScore;
	}

	public ArrayList<OldBoardState> expandSubBoardStates(OldBoardState pNodeToExpand) throws KnowledgeBaseException, ConfigurationException {
		if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
			sLogger.trace("Entering");
		}

		GameState lGameState = pNodeToExpand.getGameState();

		if (!lGameState.equals(OldBoardState.GameState.CONTINUE)) {
			sLogger.debug("Invalid GameState.  Cannot Expand Move");
			sLogger.trace("Exiting");
			return null;
		}

		ArrayList<OldBoardState> lSubBoardState = new ArrayList<OldBoardState>();

		OldBoardState.Move lMove = pNodeToExpand.getMove();
		
		KnowledgeBaseFilePool lKnowledgeBaseFilePool = pNodeToExpand.getKnowledgeBaseFilePool();

		// Current Move was an opponent move
		if (lMove == OldBoardState.Move.OPPONENT_NOMOVE || lMove == OldBoardState.Move.OPPONENT_SLOT0 || lMove == OldBoardState.Move.OPPONENT_SLOT1
				|| lMove == OldBoardState.Move.OPPONENT_SLOT2 || lMove == OldBoardState.Move.OPPONENT_SLOT3 || lMove == OldBoardState.Move.OPPONENT_SLOT4
				|| lMove == OldBoardState.Move.OPPONENT_SLOT5 || lMove == OldBoardState.Move.OPPONENT_SLOT6) {
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT0, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT0.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT1, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT1.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT2, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT2.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT3, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT3.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT4, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT4.getMoveIntValue());
				}
			}
			
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT5, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT5.getMoveIntValue());
					
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.SELF_SLOT6, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.SELF_SLOT6.getMoveIntValue());
				}
			}

			if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
				sLogger.trace("Exiting");
			}
			return lSubBoardState;
		} else if (lMove == OldBoardState.Move.SELF_NOMOVE || lMove == OldBoardState.Move.SELF_SLOT0 || lMove == OldBoardState.Move.SELF_SLOT1
				|| lMove == OldBoardState.Move.SELF_SLOT2 || lMove == OldBoardState.Move.SELF_SLOT3 || lMove == OldBoardState.Move.SELF_SLOT4
				|| lMove == OldBoardState.Move.SELF_SLOT5 || lMove == OldBoardState.Move.SELF_SLOT6) {
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT0, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT0.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT1, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT1.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT2, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT2.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT3, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT3.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT4, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT4.getMoveIntValue());
				}
			}
			
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT5, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT5.getMoveIntValue());
				}
			}
			try {
				OldBoardState lNewBoardState = new OldBoardState(lKnowledgeBaseFilePool, pNodeToExpand, OldBoardState.Move.OPPONENT_SLOT6, true, mLogContext);
				lSubBoardState.add(lNewBoardState);
			} catch (InvalidMoveException eE) {
				if (sLogger.isDebugEnabled()) {
					sLogger.debug("Invalid Evaluated Move: " + OldBoardState.Move.OPPONENT_SLOT6.getMoveIntValue());
				}
			}

			if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
				sLogger.trace("Exiting");
			}
			return lSubBoardState;

		} else {
			sLogger.error("Could not get an optimized move!");

			if (ApplicationPrecompilerSettings.TRACE_LOGACTIVE) {
				sLogger.trace("Exiting");
			}
			return null;
		}

	}
}