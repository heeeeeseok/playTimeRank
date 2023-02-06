package com.project.playtimerank.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.playtimerank.R;
import com.project.playtimerank.db.DBHelper;

import java.util.List;


public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.ViewHolder>{

    private final Context context;
    private final List<FollowInfo> followInfoList;

    public FollowAdapter(Context context, List<FollowInfo> followInfoList) {
        this.context = context;
        this.followInfoList = followInfoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.follow_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int rank = position + 1;
        holder.rank.setText(rank + "");
        holder.userName.setText(followInfoList.get(position).getUserName());
        holder.playTime.setText(followInfoList.get(position).getPlayTimeMinute() + " 분");
    }

    @Override
    public int getItemCount() {
        return followInfoList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView rank;
        TextView userName;
        TextView playTime;
        ImageButton deleteFollowBtn;
        LinearLayout followInfoLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            rank = itemView.findViewById(R.id.rank_textview);
            userName = itemView.findViewById(R.id.username_textview);
            playTime = itemView.findViewById(R.id.playtime_textview);
            followInfoLayout = itemView.findViewById(R.id.follow_info_layout);
            deleteFollowBtn = itemView.findViewById(R.id.delete_follow_imagebutton);
            deleteFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DBHelper dbHelper = new DBHelper(context);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("delete from tb_follows where puuid=" +
                            "'" + followInfoList.get(getAdapterPosition()).getPuuid() + "'");
                    followInfoList.remove(getAdapterPosition());
                    notifyDataSetChanged();
                }
            });
        }
    }
}
