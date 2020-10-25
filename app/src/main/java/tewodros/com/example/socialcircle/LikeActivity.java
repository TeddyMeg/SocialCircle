package tewodros.com.example.socialcircle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class LikeActivity extends AppCompatActivity {
    private RecyclerView likeResultList;

    private Toolbar mToolBar;
    private String postKey;
    private DatabaseReference usersRef,postsRef,likesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like);

        mToolBar = findViewById(R.id.like_appbar_layout);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setIcon(getDrawable(R.drawable.social_circle_app_icon));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("People who liked this post");

        likeResultList = findViewById(R.id.like_result_list);
        likeResultList.setHasFixedSize(true);
        likeResultList.setLayoutManager(new LinearLayoutManager(this));

        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        postKey = getIntent().getExtras().get("PostKey").toString();

        DisplayPeopleWhoLikedPost();

    }

    private void DisplayPeopleWhoLikedPost() {
        FirebaseRecyclerOptions<Likes> options = new FirebaseRecyclerOptions.Builder<Likes>()
                .setQuery(likesRef.child(postKey),Likes.class).build();
        FirebaseRecyclerAdapter<Likes,LikeViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Likes, LikeViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull LikeViewHolder holder, int position, @NonNull Likes model) {
                holder.myName.setText(model.getFullname());
                Picasso.get().load(model.getProfileimage()).placeholder(R.drawable.profile).into(holder.myImage);
            }

            @NonNull
            @Override
            public LikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.like_users_display_layout,parent,false);
                LikeViewHolder holder = new LikeViewHolder(view);
                return holder;
            }
        };
        likeResultList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }
    public static class LikeViewHolder extends RecyclerView.ViewHolder{
        View mView;
        CircleImageView myImage;
        TextView myName;

        public LikeViewHolder(@NonNull View itemView) {
            super(itemView);
            this.mView = itemView;
            myImage = mView.findViewById(R.id.like_users_profile_image);
            myName = mView.findViewById(R.id.like_users_profile_name);

        }
    }

}
