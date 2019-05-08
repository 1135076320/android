package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;


public class MegaExplorerLollipopAdapter extends RecyclerView.Adapter<MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop> implements View.OnClickListener, View.OnLongClickListener, SectionTitleProvider {
	
	final public static int CLOUD_EXPLORER = 0;
	final public static int INCOMING_SHARES_EXPLORER = 1;

	public static int MAX_WIDTH_FILENAME_LAND=500;
	public static int MAX_WIDTH_FILENAME_PORT=235;

	Context context;
	MegaApiAndroid megaApi;
	MegaPreferences prefs;


	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	ArrayList<MegaNode> nodes;

	DatabaseHandler dbH = null;
	private ArrayList<Long> disabledNodes;

	Object fragment;

	long parentHandle = -1;
	boolean selectFile = false;

	boolean multipleSelect;
	private SparseBooleanArray selectedItems;

	RecyclerView listFragment;

    /*public static view holder class*/
    public class ViewHolderExplorerLollipop extends RecyclerView.ViewHolder{
		public RelativeLayout itemLayout;
		public int currentPosition;
		public long document;


    	public ViewHolderExplorerLollipop(View itemView) {
			super(itemView);
		}
    }

	public class ViewHolderListExplorerLollipop extends ViewHolderExplorerLollipop{
		public ImageView imageView;
		public ImageView permissionsIcon;
        public TextView textViewFileName;
		public TextView textViewFileSize;


		public ViewHolderListExplorerLollipop(View itemView) {
			super(itemView);
		}
	}

	public class ViewHolderGridExplorerLollipop extends ViewHolderExplorerLollipop{
		public RelativeLayout folderLayout;
		public RelativeLayout thumbnailFolderLayout;
		public ImageView folderIcon;
		public TextView folderName;
		public RelativeLayout fileLayout;
        public RelativeLayout thumbnailFileLayout;
        public ImageView fileThumbnail;
        public ImageView fileSelectedIcon;
        public ImageView fileIcon;
        public TextView fileName;
        public RelativeLayout videoLayout;
        public TextView videoDuration;
        public ImageView videoIcon;


		public ViewHolderGridExplorerLollipop(View itemView) {
			super(itemView);
		}
	}
	
	ViewHolderExplorerLollipop holder = null;    

	public MegaExplorerLollipopAdapter(Context _context, Object fragment, ArrayList<MegaNode> _nodes, long _parentHandle, RecyclerView listView, boolean selectFile){
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.listFragment = listView;
		this.selectFile = selectFile;
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		this.fragment = fragment;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);

	}
	
	@Override
	public int getItemCount() {
		if (nodes == null){
			nodes = new ArrayList<MegaNode>();
		}

		return nodes.size();
	}

	public Object getItem(int position) {
		return nodes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public ViewHolderExplorerLollipop onCreateViewHolder(ViewGroup parent, int viewType) {

	    listFragment = (RecyclerView) parent;

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		View v;
	    if (((FileExplorerActivityLollipop) context).isList()) {
	        log("onCreateViewHolder list");
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer, parent, false);
			ViewHolderListExplorerLollipop holder = new ViewHolderListExplorerLollipop(v);

			holder.itemLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_item_layout);
			holder.imageView = (ImageView) v.findViewById(R.id.file_explorer_thumbnail);
			holder.textViewFileName = (TextView) v.findViewById(R.id.file_explorer_filename);
			holder.textViewFileSize = (TextView) v.findViewById(R.id.file_explorer_filesize);
			holder.permissionsIcon = (ImageView) v.findViewById(R.id.file_explorer_permissions);

			v.setTag(holder);
			return holder;
		}
		else {
		    log("onCreateViewHolder grid");
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer_grid, parent, false);
			ViewHolderGridExplorerLollipop holder =  new ViewHolderGridExplorerLollipop(v);

			holder.itemLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_grid_layout);
            holder.folderLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_grid_folder_layout);
            holder.thumbnailFolderLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_grid_folder_thumbnail_layout);
            holder.folderIcon = (ImageView) v.findViewById(R.id.file_explorer_grid_folder_icon);
            holder.folderName= (TextView) v.findViewById(R.id.file_explorer_grid_folder_filename);
            holder.fileLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_grid_file_layout);
            holder.thumbnailFileLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_grid_file_thumbnail_layout);
            holder.fileThumbnail = (ImageView) v.findViewById(R.id.file_explorer_grid_file_thumbnail);
            holder.fileSelectedIcon = (ImageView) v.findViewById(R.id.file_explorer_grid_file_selected);
            holder.fileIcon = (ImageView) v.findViewById(R.id.file_explorer_grid_file_icon);
            holder.fileName= (TextView) v.findViewById(R.id.file_grid_filename_for_file);
            holder.videoLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_grid_file_videoinfo_layout);
            holder.videoDuration= (TextView) v.findViewById(R.id.file_explorer_grid_file_title_video_duration);
            holder.videoIcon = (ImageView) v.findViewById(R.id.file_explorer_grid_file_video_icon);

			v.setTag(holder);
			return holder;
		}
	}

	@Override
	public void onBindViewHolder(ViewHolderExplorerLollipop holder, int position){
        if (((FileExplorerActivityLollipop) context).isList()) {
            ViewHolderListExplorerLollipop holderList = (ViewHolderListExplorerLollipop) holder;
            onBindViewHolderList(holderList, position);
        }
        else  {
            ViewHolderGridExplorerLollipop holderGrid = (ViewHolderGridExplorerLollipop) holder;
            onBindViewHolderGrid(holderGrid, position);
        }
	}

	private void setImageParams (ImageView image, int size, int sizeLeft) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) image.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeLeft, context.getResources().getDisplayMetrics());
        params.setMargins(left, 0, 0, 0);

        image.setLayoutParams(params);
    }

    public void onBindViewHolderList(ViewHolderListExplorerLollipop holder, int position) {
	    log("onBindViewHolderList");
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        holder.currentPosition = position;

        MegaNode node = (MegaNode) getItem(position);
        if (node == null) {
            return;
        }
        holder.document = node.getHandle();

        holder.textViewFileName.setText(node.getName());

        if (node.isFolder()){
            setImageParams(holder.imageView, 48, 0);
            holder.itemLayout.setBackgroundColor(Color.WHITE);
            holder.itemView.setOnLongClickListener(null);

            if (disabledNodes != null){

                if (disabledNodes.contains(node.getHandle())){
                    log("Disabled!");
                    holder.imageView.setAlpha(.4f);
                    holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    holder.permissionsIcon.setAlpha(.2f);
                    holder.itemView.setOnClickListener(null);
                }
                else{
                    log("Full access");
                    holder.imageView.setAlpha(1.0f);
                    holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                    holder.itemView.setOnClickListener(this);
                    holder.permissionsIcon.setAlpha(.35f);
                }
            }
            else{
                holder.imageView.setAlpha(1.0f);
                holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                holder.itemView.setOnClickListener(this);
            }

            if(node.isInShare()){
                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.textViewFileName.setMaxWidth(Util.scaleWidthPx(260, outMetrics));
                    holder.textViewFileSize.setMaxWidth(Util.scaleWidthPx(260, outMetrics));

                }
                else{
                    holder.textViewFileName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
                    holder.textViewFileSize.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

                }
                holder.imageView.setImageResource(R.drawable.ic_folder_incoming_list);
                ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                for(int j=0; j<sharesIncoming.size();j++){
                    MegaShare mS = sharesIncoming.get(j);
                    if(mS.getNodeHandle()==node.getHandle()){
                        MegaUser user= megaApi.getContact(mS.getUser());
                        if(user!=null){
                            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                            if(contactDB!=null){
                                if(!contactDB.getName().equals("")){
                                    holder.textViewFileSize.setText(contactDB.getName()+" "+contactDB.getLastName());
                                }
                                else{
                                    holder.textViewFileSize.setText(user.getEmail());
                                }
                            }
                            else{
                                log("The contactDB is null: ");
                                holder.textViewFileSize.setText(user.getEmail());
                            }
                        }
                        else{
                            holder.textViewFileSize.setText(mS.getUser());
                        }
                    }
                }

                //Check permissions
                holder.permissionsIcon.setVisibility(View.VISIBLE);
                int accessLevel = megaApi.getAccess(node);

                if(accessLevel== MegaShare.ACCESS_FULL){
                    holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                }
                else if(accessLevel== MegaShare.ACCESS_READ){
                    holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read);
                }
                else{
                    holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
                }
            }
            else if(node.isOutShare()||megaApi.isPendingShare(node)) {
                holder.permissionsIcon.setVisibility(View.GONE);
                holder.imageView.setImageResource(R.drawable.ic_folder_outgoing_list);
                holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context));

            }else{
                holder.permissionsIcon.setVisibility(View.GONE);
                boolean isCU = isCameraUploads(node);
                if(isCU){
                    holder.imageView.setImageResource(R.drawable.ic_folder_image_list);
                }else{
                    holder.imageView.setImageResource(R.drawable.ic_folder_list);
                }
                holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context));
            }
        }
        else{
            holder.permissionsIcon.setVisibility(View.GONE);

            long nodeSize = node.getSize();
            holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
            holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            setImageParams(holder.imageView, 48, 0);

            if(selectFile){
                holder.imageView.setAlpha(1.0f);
                holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                holder.itemView.setOnClickListener(this);
                holder.itemView.setOnLongClickListener(this);

                if (multipleSelect) {
                    if(this.isItemChecked(position)){
                        holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        holder.imageView.setImageResource(R.drawable.ic_select_folder);
                        log("Do not show thumb");
                        return;
                    }
                    else{
                        holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        holder.itemLayout.setBackgroundColor(Color.WHITE);
                    }
                }
                else{
                    holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    holder.itemLayout.setBackgroundColor(Color.WHITE);
                }
            }
            else{
                holder.imageView.setAlpha(.4f);
                holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
            }


            Bitmap thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
            if (thumb == null) {
                thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
                if (thumb == null) {
                    try {
                        if (node.hasThumbnail()) {
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaExplorerLollipop(node, context, holder, megaApi, this);
                        }
                        else {
                            ThumbnailUtilsLollipop.createThumbnailExplorerLollipop(context, node, holder, megaApi, this);
                        }
                    } catch (Exception e) {}
                }
            }

            if (thumb != null) {
                setImageParams(holder.imageView, 36, 6);
                holder.imageView.setImageBitmap(thumb);
            }
        }
    }

    public void onBindViewHolderGrid(ViewHolderGridExplorerLollipop holder, int position) {
	    log("onBindViewHolderGrid");
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        holder.currentPosition = position;

        MegaNode node = (MegaNode) getItem(position);

        if (node == null) {
            holder.fileLayout.setVisibility(View.GONE);
            holder.folderLayout.setVisibility(View.INVISIBLE);
            holder.itemLayout.setVisibility(View.INVISIBLE);
            return;
        }

        holder.document = node.getHandle();
        holder.itemLayout.setVisibility(View.VISIBLE);

        if (node.isFolder()){
            holder.folderLayout.setVisibility(View.VISIBLE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.folderName.setText(node.getName());

            holder.itemLayout.setBackgroundColor(Color.WHITE);
            holder.itemView.setOnLongClickListener(null);

            holder.folderName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            holder.itemView.setOnClickListener(this);

            if(node.isInShare()){
                holder.folderIcon.setImageResource(R.drawable.ic_folder_incoming_list);
            }
            else if(node.isOutShare()||megaApi.isPendingShare(node)) {
                holder.folderIcon.setImageResource(R.drawable.ic_folder_outgoing_list);
            }
            else if(isCameraUploads(node)){
                holder.folderIcon.setImageResource(R.drawable.ic_folder_image_list);
            }
            else{
                holder.folderIcon.setImageResource(R.drawable.ic_folder_list);
            }
        }
        else{
            holder.itemView.setOnClickListener(this);
            holder.itemView.setOnLongClickListener(this);
            holder.folderLayout.setVisibility(View.GONE);
            holder.fileLayout.setVisibility(View.VISIBLE);
            holder.fileName.setText(node.getName());
            holder.fileThumbnail.setVisibility(View.GONE);
            holder.fileIcon.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());

            if (Util.isVideoFile(node.getName())) {
                holder.videoLayout.setVisibility(View.VISIBLE);
                log(node.getName() + " DURATION: " + node.getDuration());
                String duration = Util.getVideoDuration(node.getDuration());
                if (duration != null && !duration.isEmpty())  {
                    holder.videoDuration.setText(duration);
                    holder.videoDuration.setVisibility(View.VISIBLE);
                }
                else {
                    holder.videoDuration.setVisibility(View.GONE);
                }
            }
            else {
                holder.videoLayout.setVisibility(View.GONE);
            }

            Bitmap thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
            if (thumb == null) {
                thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
                if (thumb == null) {
                    try {
                        if (node.hasThumbnail()) {
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaExplorerLollipop(node, context, holder, megaApi, this);
                        }
                        else {
                            ThumbnailUtilsLollipop.createThumbnailExplorerLollipop(context, node, holder, megaApi, this);
                        }
                    } catch (Exception e) {}
                }
            }
            if (thumb != null) {
                holder.fileThumbnail.setImageBitmap(ThumbnailUtilsLollipop.getRoundedRectBitmap(context,thumb,2));
                holder.fileThumbnail.setVisibility(View.VISIBLE);
                holder.fileIcon.setVisibility(View.GONE);
            }

            if (isMultipleSelect() && isItemChecked(position)) {
                holder.itemLayout.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid_selected));
                holder.fileSelectedIcon.setImageResource(R.drawable.ic_select_folder);

            } else {
                holder.itemLayout.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid));
                holder.fileSelectedIcon.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    public void toggleAllSelection(int pos) {
        log("toggleAllSelection: " + pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

    public void toggleSelection(int pos) {
        log("toggleSelection: " + pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

	boolean putOrDeletePostion(int pos) {
        if (selectedItems.get(pos,false)) {
            log("delete pos: " + pos);
            selectedItems.delete(pos);
            return true;
        } else {
            log("PUT pos: " + pos);
            selectedItems.put(pos,true);
            return false;
        }
    }

    private void hideMultipleSelect() {
        if (selectedItems.size() <= 0) {
            if (fragment instanceof CloudDriveExplorerFragmentLollipop) {
                ((CloudDriveExplorerFragmentLollipop) fragment).hideMultipleSelect();
            } else if (fragment instanceof IncomingSharesExplorerFragmentLollipop) {
                ((IncomingSharesExplorerFragmentLollipop) fragment).hideMultipleSelect();
            }
        }
    }

    void startAnimation (final int pos, final boolean delete) {

        if (((FileExplorerActivityLollipop) context).isList()) {
            log("adapter type is LIST");
            ViewHolderListExplorerLollipop view = (ViewHolderListExplorerLollipop) listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                log("Start animation: " + pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context,R.anim.multiselect_flip);
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (!delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        hideMultipleSelect();
                        if (delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.imageView.startAnimation(flipAnimation);
            }
            else {
                log("view is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        } else {
            log("adapter type is GRID");
            ViewHolderGridExplorerLollipop view = (ViewHolderGridExplorerLollipop) listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                log("Start animation: " + pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context,R.anim.multiselect_flip);
                if (!delete) {
                    notifyItemChanged(pos);
                    flipAnimation.setDuration(250);
                }
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (!delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        hideMultipleSelect();
                        notifyItemChanged(pos);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.fileSelectedIcon.startAnimation(flipAnimation);
            }
            else {
                log("view is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        }
    }

	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				if (fragment instanceof CloudDriveExplorerFragmentLollipop) {
					if(!((CloudDriveExplorerFragmentLollipop) fragment).isFolder(i)){
						toggleAllSelection(i);
					}
				}
				else if (fragment instanceof IncomingSharesExplorerFragmentLollipop) {
					if(!((IncomingSharesExplorerFragmentLollipop) fragment).isFolder(i)){
						toggleAllSelection(i);
					}
				}

			}
		}
	}

	public void clearSelections() {
		log("clearSelections");
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}

	private boolean isItemChecked(int position) {
	    if (selectedItems == null) {
	        return false;
        }
		return selectedItems.get(position);
	}

	public int getSelectedItemCount() {
		if(selectedItems!=null){
			return selectedItems.size();
		}
		return 0;
	}

	public List<Integer> getSelectedItems() {
		List<Integer> items = new ArrayList<Integer>(selectedItems.size());
		for (int i = 0; i < selectedItems.size(); i++) {
			items.add(selectedItems.keyAt(i));
		}
		return items;
	}

	/*
	 * Get list of all selected nodes
	 */
	public List<MegaNode> getSelectedNodes() {
		ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();

		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaNode document = getNodeAt(selectedItems.keyAt(i));
				if (document != null){
					nodes.add(document);
				}
			}
		}
		return nodes;
	}

	public long[] getSelectedHandles() {

		long handles[] = new long[selectedItems.size()];

		int k=0;
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaNode document = getNodeAt(selectedItems.keyAt(i));
				if (document != null){
					handles[k] = document.getHandle();
					k++;
				}
			}
		}
		return handles;
	}

	/*
 * Get document at specified position
 */
	public MegaNode getNodeAt(int position) {
		try {
			if (nodes != null) {
				return nodes.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}

	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		log("setMultipleSelect");
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
		}
		if(this.multipleSelect){
			selectedItems = new SparseBooleanArray();
		}
	}

	public boolean isEnabled(int position) {
		if (nodes.size() == 0){
			return false;
		}
		
		MegaNode document = nodes.get(position);
		if (document.isFile()){
			if(selectFile){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			if (disabledNodes != null) {
				if (disabledNodes.contains(document.getHandle())){
					return false;
				}
			}
		}
		
		return true;
	}

	public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = insertPlaceHolderNode(nodes);
		positionClicked = -1;
		notifyDataSetChanged();
		visibilityFastScroller();
	}

	public long getParentHandle(){
		return parentHandle;
	}

	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
	}
	
	/*
	 * Set provided nodes disabled
	 */
	public void setDisableNodes(ArrayList<Long> disabledNodes) {
		this.disabledNodes = disabledNodes;
	}

	public boolean isSelectFile() {
		return selectFile;
	}

	public void setSelectFile(boolean selectFile) {
		this.selectFile = selectFile;
	}

	private static void log(String log) {
		Util.log("MegaExplorerLollipopAdapter", log);
	}


	public boolean isCameraUploads(MegaNode n){
		log("isCameraUploads()");
		String cameraSyncHandle = null;

		//Check if the item is the Camera Uploads folder
		if(dbH.getPreferences()!=null){
			prefs = dbH.getPreferences();
			if(prefs.getCamSyncHandle()!=null){
				cameraSyncHandle = prefs.getCamSyncHandle();
			}else{
				cameraSyncHandle = null;
			}
		}else{
			prefs=null;
		}

		if(cameraSyncHandle!=null){
			if(!(cameraSyncHandle.equals(""))){
				if ((n.getHandle()==Long.parseLong(cameraSyncHandle))){
					return true;
				}

			}else{
				if(n.getName().equals("Camera Uploads")){
					if (prefs != null){
						prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
					}
					dbH.setCamSyncHandle(n.getHandle());
					log("FOUND Camera Uploads!!----> "+n.getHandle());
					return true;
				}
			}

		}else{
			if(n.getName().equals("Camera Uploads")){
				if (prefs != null){
					prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
				}
				dbH.setCamSyncHandle(n.getHandle());
				log("FOUND Camera Uploads!!: "+n.getHandle());
				return true;
			}
		}

		//Check if the item is the Media Uploads folder
		String secondaryMediaHandle = null;

		if(prefs!=null){
			if(prefs.getMegaHandleSecondaryFolder()!=null){
				secondaryMediaHandle =prefs.getMegaHandleSecondaryFolder();
			}else{
				secondaryMediaHandle = null;
			}
		}

		if(secondaryMediaHandle!=null){
			if(!(secondaryMediaHandle.equals(""))){
				if ((n.getHandle()==Long.parseLong(secondaryMediaHandle))){
					log("Click on Media Uploads");
					return true;
				}
			}
		}else{
			if(n.getName().equals(CameraSyncService.SECONDARY_UPLOADS)){
				if (prefs != null){
					prefs.setMegaHandleSecondaryFolder(String.valueOf(n.getHandle()));
				}
				dbH.setSecondaryFolderHandle(n.getHandle());
				log("FOUND Media Uploads!!: "+n.getHandle());
				return true;
			}
		}
		return false;
	}



	@Override
	public void onClick(View v) {
		clickItem(v);
	}

	@Override
	public boolean onLongClick(View v) {
		clickItem(v);
		return true;
	}

	void clickItem(View v) {
		ViewHolderExplorerLollipop holder = (ViewHolderExplorerLollipop) v.getTag();
		if (holder == null) {
			return;
		}

		if (fragment instanceof CloudDriveExplorerFragmentLollipop)  {
			((CloudDriveExplorerFragmentLollipop) fragment).itemClick(v, holder.getAdapterPosition());
		}
		else if (fragment instanceof IncomingSharesExplorerFragmentLollipop) {
			((IncomingSharesExplorerFragmentLollipop) fragment).itemClick(v, holder.getAdapterPosition());
		}
	}

    private ArrayList<MegaNode> insertPlaceHolderNode(ArrayList<MegaNode> nodes) {
	    if (nodes == null) {
	        return null;
        }
        int folderCount = 0;
        for (MegaNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            }
        }
        int spanCount = 2;
        if (listFragment instanceof NewGridRecyclerView) {
            spanCount = ((NewGridRecyclerView)listFragment).getSpanCount();
        }
        int placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);

        if (folderCount > 0 && placeholderCount != 0 && !((FileExplorerActivityLollipop) context).isList()) {
            //Add placeholder at folders' end.
            for (int i = 0;i < placeholderCount;i++) {
                nodes.add(folderCount + i,null);
            }
        }
        return nodes;
    }

    private void visibilityFastScroller() {
	    int visibility;
        if (getItemCount() < Constants.MIN_ITEMS_SCROLLBAR) {
            visibility = View.GONE;
        }
        else {
            visibility = View.VISIBLE;
        }

        if (fragment instanceof IncomingSharesExplorerFragmentLollipop) {
            ((IncomingSharesExplorerFragmentLollipop) fragment).getFastScroller().setVisibility(visibility);
        }
        else if (fragment instanceof CloudDriveExplorerFragmentLollipop) {
            ((CloudDriveExplorerFragmentLollipop) fragment).getFastScroller().setVisibility(visibility);
        }
    }

    @Override
    public String getSectionTitle(int position) {
	    MegaNode node = (MegaNode) getItem(position);

        if (node != null && node.getName() != null && !node.getName().isEmpty()) {
            return node.getName().substring(0,1);
        }
        return null;
    }
}
