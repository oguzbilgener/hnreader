package me.oguzb.hnreader.comments;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oguzdev.hnclient.CommentItem;
import com.oguzdev.hnclient.NewsItem;

import java.util.List;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.utils.Utils;

public class CommentsListAdapter extends ArrayAdapter<CommentItem> implements View.OnClickListener
{
    private final Context context;
    private final List<CommentItem> list;
    private LayoutInflater inflater;

	private final static int layoutId = R.layout.comment_item;
	private final static int opId = R.layout.comment_original_post;

	public final static int VIEW_TYPE_COMMENT = 0;
	public final static int VIEW_TYPE_OP = 1;

    public CommentsListAdapter(Context con, List<CommentItem> list)
    {
        super(con, layoutId, list);
        this.context = con;
        this.list = list;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setNotifyOnChange(false);
    }


    public static class ViewHolder
    {
        protected TextView commentUser;
        protected TextView commentText;
        protected TextView commentTime;

        protected LinearLayout commentDepth;
    }

	public static class OpHolder
	{
		protected TextView titleText;
		protected TextView ownerText;
		protected TextView bodyText;
		protected TextView domainText;
		protected TextView commentsText;
		protected TextView pointsText;
		protected TextView timeText;
	}

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;

        if(convertView == null)
        {
			if(getItemViewType(position) == VIEW_TYPE_COMMENT)
			{
				view = inflater.inflate(layoutId, null);
				final ViewHolder viewHolder = new ViewHolder();

				viewHolder.commentUser = (TextView) view.findViewById(R.id.commentUser);
				viewHolder.commentText = (TextView) view.findViewById(R.id.commentText);
				viewHolder.commentTime = (TextView) view.findViewById(R.id.commentTime);

				viewHolder.commentDepth = (LinearLayout) view.findViewById(R.id.comment_depth_indicator);

				view.setTag(R.id.commentitem_object,viewHolder);
			}
			else
			{
				view = inflater.inflate(opId, null);
				final OpHolder opHolder = new OpHolder();
				opHolder.titleText = (TextView) view.findViewById(R.id.op_titleText);
				opHolder.ownerText = (TextView) view.findViewById(R.id.op_ownerText);
				opHolder.bodyText = (TextView) view.findViewById(R.id.op_bodyText);
				opHolder.domainText = (TextView) view.findViewById(R.id.op_domainText);
				opHolder.commentsText= (TextView) view.findViewById(R.id.op_commentsText);
				opHolder.pointsText = (TextView) view.findViewById(R.id.op_pointsText);
				opHolder.timeText = (TextView) view.findViewById(R.id.op_timeText);

				view.setTag(R.id.op_object, opHolder);
			}
        }
        else
        {
            view = convertView;
        }

        // Get our list item in this specific position
        CommentItem item = list.get(position);
        if(item == null)
        {
            Utils.log.w("CommentsList["+position+"] is null.");
            return view;
        }

		if(getItemViewType(position) == VIEW_TYPE_COMMENT)
		{
			final ViewHolder holder = (ViewHolder) view.getTag(R.id.commentitem_object);
			// Set tags to parent view and some other views
			// so that we can handle events like onclick easily
			view.setTag(R.id.commentitem_depth, item.depth());
			view.setTag(R.id.commentitem_index, position);
			view.setTag(R.id.commentitem_hidden, false);

			// Set values for list item's views
			holder.commentUser.setText(item.getUsername());
			holder.commentText.setText(Html.fromHtml(item.getCommentText()));
			holder.commentTime.setText(item.getCommentTime());

			// Show comment depth
			// set color
			holder.commentDepth.setBackgroundColor(
					context.getResources().getColor(getDepthColorResourceByDepth(item.depth())));
			// set width
			try
			{
				ViewGroup.LayoutParams depthParams = holder.commentDepth.getLayoutParams();
				depthParams.width = (int) Utils.getPixelsByDp(context, (float) item.depth()*10);
				holder.commentDepth.setLayoutParams(depthParams);
			}
			catch(Exception e)
			{
				Utils.log.w("Could not set LayoutParams: "+e.toString()+":"+e.getMessage());
				e.printStackTrace();
			}

			// Set onClickListener for comment root
			view.setClickable(true);
			view.setOnClickListener(this);
		}
		else
		{
			// Set up the original post view
			final OpHolder opHolder = (OpHolder) view.getTag(R.id.op_object);

			NewsItem opObject = item.getOpObject();
			// opObject must not be null
			if(opObject == null)
			{
				view.setVisibility(View.GONE);
				return view;
			}
			else
			{
				view.setVisibility(View.VISIBLE);
				if(opObject.getTitle() != null && !opObject.getTitle().equals(""))
					opHolder.titleText.setText(opObject.getTitle());
				if(opObject.getUsername() != null && !opObject.getUsername().equals(""))
				{
					opHolder.ownerText.setVisibility(View.VISIBLE);
					opHolder.ownerText.setText(opObject.getUsername());
				}
				else
				{
					opHolder.ownerText.setVisibility(View.VISIBLE);
				}
				if(opObject.getBodyText() != null & !opObject.getBodyText().equals(""))
				{
					opHolder.bodyText.setVisibility(View.VISIBLE);
					opHolder.bodyText.setText(Html.fromHtml(opObject.getBodyText()));
				}
				else
				{
					opHolder.bodyText.setVisibility(View.GONE);
				}
				if(opObject.getDomain() != null && !opObject.getDomain().equals(""))
					opHolder.domainText.setText(opObject.getDomain());
				if(opObject.getComments() != null && !opObject.getComments().equals(""))
					opHolder.commentsText.setText(opObject.getComments());
				if(opObject.getPoints() != null && !opObject.getPoints().equals(""))
					opHolder.pointsText.setText(opObject.getPoints());
				if(opObject.getTime() != null && !opObject.getTime().equals(""))
					opHolder.timeText.setText(opObject.getTime());
			}
		}

        return view;
    }

	@Override
	public int getItemViewType(int position)
	{
		if(position==0)
			return VIEW_TYPE_OP;
		else
			return VIEW_TYPE_COMMENT;
	}

	@Override
	public int getViewTypeCount()
	{
		return 2;
	}

    @Override
    public void onClick(View view)
    {
        switch(view.getId())
        {
            case R.id.comment_parent:

            break;
        }
    }

    public int getDepthColorResourceByDepth(int depth)
    {
        switch(depth)
        {
            case 0:
                return R.color.c_depth_0;
            case 1:
                return R.color.c_depth_1;
            case 2:
                return R.color.c_depth_2;
            case 3:
                return R.color.c_depth_3;
            case 4:
                return R.color.c_depth_4;
            case 5:
                return R.color.c_depth_5;
            case 6:
                return R.color.c_depth_6;
            case 7:
                return R.color.c_depth_7;
            case 8:
                return R.color.c_depth_8;
            case 9:
            default:
                return R.color.c_depth_9;
        }

    }
}
