/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Aleksander V. Budniy

 /**
 * Created on 15.08.2005
 */
package org.apache.harmony.jpda.tests.jdwp.StackFrame;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for StackFrame.GetValues command.
 */
public class GetValuesTest extends JDWPStackFrameTestCase {

    String testedMethodName = "nestledMethod3";
    String testedThreadName = "";

    VarInfo[] varInfos;

    String varSignatures[] = { "Lorg/apache/harmony/jpda/tests/jdwp/StackFrame/StackTraceDebuggee;", "Z",
            "I", "Ljava/lang/String;" };

    String varNames[] = { "this", "boolLocalVariable", "intLocalVariable",
            "strLocalVariable" };

    byte varTags[] = { JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.BOOLEAN_TAG,
            JDWPConstants.Tag.INT_TAG, JDWPConstants.Tag.STRING_TAG };

    private String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/StackFrame/StackTraceDebuggee;";

    /**
     * This testcase exercises StackFrame.GetValues command.
     * <BR>The test starts StackTraceDebuggee, sets breakpoint at the beginning of
     * the tested method - 'nestledMethod3' and stops at the breakpoint.
     * <BR> Then the test performs Method.VariableTable command and checks
     * returned VariableTable.
     * <BR> At last the test performs StackFrame.GetValues command and checks
     * returned values of variables.
     *
     */
    public void testGetValues001() {
        logWriter.println("==> testGetValues001 started...");
        testedThreadName = synchronizer.receiveMessage();
        // release on run()
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // pass nestledMethod1()
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // enter nestledMethod2()
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        //release debuggee
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        //gets and checks local variables of tested method
        examineGetValues();

        // signal to finish debuggee
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> testGetValues001 - OK.");

    }

    private void examineGetValues() {

        long refTypeID = getClassIDBySignature(debuggeeSignature);
        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = "
                + refTypeID);
        long threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);

        logWriter.println("=> testedThreadID = " + threadID);
        if (threadID == -1) {
            printErrorAndFail("testedThread is not found!");
        }

        // suspend thread
        jdwpSuspendThread(threadID);

        //get number of frames
        int frameCount = jdwpGetFrameCount(threadID);
        logWriter.println("=> frames count = " + frameCount);

        //get frames info
        FrameInfo[] frameIDs = jdwpGetFrames(threadID, 0, frameCount);
        if (frameIDs.length != frameCount) {
            printErrorAndFail("Received number of frames = "
                    + frameIDs.length + " differ from expected number = "
                    + frameCount);
        }

        //check and print methods info
        long methodID = 0;
        long frameID = 0;
        String methodName = "";
        boolean testedMethodChecked = false;
        for (int i = 0; i < frameCount; i++) {
            logWriter.println("\n");
            methodName = getMethodName(frameIDs[i].location.classID,
                    frameIDs[i].location.methodID);
            logWriter.println("=> method name = " + methodName);
            logWriter.println("=> methodID = " + frameIDs[i].location.methodID);
            logWriter.println("=> frameID = " + frameIDs[i].frameID);
            logWriter.println("\n");
            if (methodName.equals(testedMethodName)) {
                methodID = frameIDs[i].location.methodID;
                frameID = frameIDs[i].frameID;
                methodName = getMethodName(frameIDs[i].location.classID,
                        frameIDs[i].location.methodID);
                testedMethodChecked = true;
            }
        }
        if (testedMethodChecked) {
            logWriter.println("=> Tested method is found");
            logWriter.println("=> method name = " + testedMethodName);
            logWriter.println("=> methodID = " + methodID);
            logWriter.println("=> frameID = " + frameID);

        } else {
            printErrorAndFail("Tested method is not found");
        }

        //getting Variable Table
        logWriter.println("");
        logWriter.println("=> Getting Variable Table...");
        varInfos = jdwpGetVariableTable(refTypeID, methodID);
        if (checkVarTable(logWriter, varInfos, varTags, varSignatures, varNames)) {
            logWriter.println("=> Variable table check passed.");
        } else {
            printErrorAndFail("Variable table check failed.");
        }

        //prepare and perform GetValues command
        logWriter.println("");
        logWriter.println("=> Send StackFrame::GetValues command...");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.GetValuesCommand);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsFrameID(frameID);

        logWriter.println("=> Thread: " + threadID);
        logWriter.println("=> Frame: " + frameID);
        packet.setNextValueAsInt(varTags.length);
        for (int i = 0; i < varTags.length; i++) {
            logWriter.println("");
            logWriter.println("=> For variable #"+i+":");
            packet.setNextValueAsInt(varInfos[i].getSlot());
            logWriter.println("=> Slot = "+varInfos[i].getSlot());
            byte tag = varTags[Arrays.asList(varNames).indexOf(varInfos[i].getName())];
            packet.setNextValueAsByte(tag);
            logWriter.println("=> Tag = "+JDWPConstants.Tag.getName(tag));
            logWriter.println("");
        }

        //check reply for errors
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "StackFrame::GetValues command");

        //check number of retrieved values
        int numberOfValues = reply.getNextValueAsInt();
        logWriter.println("=> Number of values = " + numberOfValues);
        if (numberOfValues != varTags.length) {
            printErrorAndFail("Unexpected number of values: "
                    + numberOfValues + " instead of "+varTags.length);
        }

        boolean success = true;
        //print and check values of variables
        logWriter.println("=> Values of variables: ");
        Value val;

        val = reply.getNextValueAsValue();
        if (val.getTag() == JDWPConstants.Tag.BOOLEAN_TAG) {
            logWriter.println("=>Tag is correct");
            boolean boolValue = val.getBooleanValue();
            if (boolValue) {
                logWriter.println("=> "+varInfos[1].getName() + " = " + boolValue);
                logWriter.println("");
            } else {
                logWriter
                        .printError("Unexpected value of boolean variable: "
                                + boolValue + " instead of: true");
                logWriter.printError("");
                success = false;
            }
        } else {
            logWriter.printError("Unexpected tag of variable: "
                    + JDWPConstants.Tag.getName(val.getTag()) + " instead of: boolean");
            logWriter.printError("");
            success = false;
        }

        val = reply.getNextValueAsValue();
        if (val.getTag() == JDWPConstants.Tag.INT_TAG) {
            logWriter.println("=>Tag is correct");
            int intValue = val.getIntValue();
            if (intValue == -512) {
                logWriter.println("=> "+varInfos[2].getName() + " = " + intValue);
                logWriter.println("");
            } else {
                logWriter
                        .printError("unexpected value of int variable: "
                                + intValue + " instead of: -512");
                logWriter.printError("");
                success = false;
            }
        } else {
            logWriter.printError("Unexpected tag of variable: "
                    + JDWPConstants.Tag.getName(val.getTag()) + " instead of: integer");
            logWriter.printError("");
            success = false;
        }

        val = reply.getNextValueAsValue();
        if (val.getTag() == JDWPConstants.Tag.STRING_TAG) {
            logWriter.println("=>Tag is correct");
            long strLocalVariableID = val.getLongValue();
            String strLocalVariable = getStringValue(strLocalVariableID);
            if (strLocalVariable.equals("test string")) {
                logWriter.println("=> "+varInfos[2].getName() + " = "
                        + strLocalVariable);
                logWriter.println("");
            } else {
                logWriter
                        .printError("Unexpected value of string variable: "
                                + strLocalVariable
                                + " instead of: "
                                + "test string");
                logWriter.printError("");
                success = false;
            }
        } else {
            logWriter.printError("Unexpected tag of variable: "
                    + JDWPConstants.Tag.getName(val.getTag()) + " instead of: string");
            logWriter.printError("");
            success = false;
        }

        val = reply.getNextValueAsValue();
        if (val.getTag() == JDWPConstants.Tag.OBJECT_TAG) {
            logWriter.println("=> Tag is correct");
            logWriter.println("");

        } else {
            logWriter.printError("Unexpected tag of variable: "
                    + JDWPConstants.Tag.getName(val.getTag()) + " instead of: CLASS_OBJECT_TAG");
            logWriter.printError("");
            success = false;
        }

        assertTrue(logWriter.getErrorMessage(), success);
    }

    //prints variables info, checks signatures
    public static boolean checkVarTable(LogWriter logWriter, VarInfo[] varInfos, byte[] varTags, String[] varSignatures, String[] varNames) {
        logWriter.println("==> Number of variables = " + varInfos.length);
        if (varInfos.length != varTags.length) {

            logWriter.printError("Unexpected number of variables: "
                    + varInfos.length + " instead of " + varTags.length);
            return false;
        }
        boolean success = true;
        ArrayList<String> remaining = new ArrayList(Arrays.asList(varNames));
        for (int i = 0; i < varInfos.length; i++) {
            logWriter.println("");
            logWriter.println("=> Name = " + varInfos[i].getName());
            logWriter.println("=> Slot = " + varInfos[i].getSlot());
            logWriter.println("=> Signature = " + varInfos[i].getSignature());
            // TODO: check codeIndex and length too

            int index = remaining.indexOf(varInfos[i].getName());
            if (index == -1) {
                logWriter.printError("unknown variable: " + varInfos[i].getName());
            }
            remaining.set(index, null);

            if (!(varSignatures[index].equals(varInfos[i].getSignature()))) {
                logWriter
                        .printError("Unexpected signature of variable = "
                                + varInfos[i].getName()
                                + ", on slot = "
                                + varInfos[i].getSlot()
                                + ", with unexpected signature = "
                                + varInfos[i].getSignature()
                                + " instead of signature = " + varSignatures[index]);
                success = false;
            }
            if (!(varNames[index].equals(varInfos[i].getName()))) {
                logWriter.printError("Unexpected name of variable  "
                        + varInfos[i].getName() + ", on slot = "
                        + varInfos[i].getSlot() + " instead of name = "
                        + varNames[index]);
                success = false;
            }

            logWriter.println("");
        }
        return success;
    }
}
