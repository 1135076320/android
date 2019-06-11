package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.CHAT_FOLDER;

/**
 * Created by mega on 19/09/18.
 */

public class CopyAndSendToChatListener implements MegaRequestListenerInterface {

    private Context context;

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    private int counter = 0;
    private MegaNode parentNode;
    private ArrayList<MegaNode> nodesCopied = new ArrayList<>();
    private ArrayList<MegaNode> preservedNotOwnerNode;

    public CopyAndSendToChatListener(Context context) {
        super();
        this.context = context;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        counter = 0;
        parentNode = megaApi.getNodeByPath("/" + CHAT_FOLDER);
    }

    private boolean isMyChatFilesExist(ArrayList<MegaNode> nodes) {
        if (parentNode == null) {
            megaApi.createFolder(CHAT_FOLDER, megaApi.getRootNode(), this);
            preservedNotOwnerNode = nodes;
            return false;
        }
        return true;
    }

    public void copyNode(MegaNode node) {
        megaApi.copyNode(node, parentNode, this);
    }

    public void copyNodes (ArrayList<MegaNode> nodes, ArrayList<MegaNode> ownerNodes) {
        nodesCopied.addAll(ownerNodes);
        counter = nodes.size();
        if (isMyChatFilesExist(nodes)) {
            for (MegaNode node : nodes) {
                copyNode(node);
            }
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_COPY){
            counter --;
            if (e.getErrorCode() == MegaError.API_OK){
                nodesCopied.add(megaApi.getNodeByHandle(request.getNodeHandle()));
                if (counter == 0){
                    if (nodesCopied != null) {
                        NodeController nC = new NodeController(context);
                        nC.selectChatsToSendNodes(nodesCopied);
                    }
                }
            }
            else {
                log("MegaRequest.TYPE_COPY error: " +e.getErrorString());
            }
        } else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER
                && CHAT_FOLDER.equals(request.getName())) {
            if (e.getErrorCode() == MegaError.API_OK){
                log("create My Chat Files folder successfully and copy the reserved nodes");
                parentNode = megaApi.getNodeByPath("/" + CHAT_FOLDER);
                if (this.preservedNotOwnerNode != null) {
                    for (MegaNode node : preservedNotOwnerNode) {
                        copyNode(node);
                    }
                }
                this.preservedNotOwnerNode = null;
            }
            else{
                log("Cannot create My Chat Files"+e.getErrorCode()+" "+e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporaryError");
    }

    private static void log(String log) {
        Util.log("CopyAndSendToChatListener", log);
    }
}
