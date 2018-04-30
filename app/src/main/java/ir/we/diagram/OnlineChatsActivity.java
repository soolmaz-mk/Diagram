package ir.we.diagram;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.telegram.messenger.ImageReceiver;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class OnlineChatsActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_online_chats);
        RecyclerView chatsView = findViewById(R.id.chats);
        chatsView.setAdapter(new ChatsAdapter(new ArrayList<TLRPC.Chat>()));
    }

    static class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatsHolder> {

        List<TLRPC.Chat> chats;

        public ChatsAdapter(List<TLRPC.Chat> chats) {
            this.chats = chats;
        }

        @Override
        public ChatsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_chat, parent, false);
            return new ChatsHolder(v);
        }

        @Override
        public void onBindViewHolder(ChatsHolder holder, int position) {
            holder.title.setText(chats.get(position).title);
            holder.receiver.setImage(chats.get(position).photo.photo_small, "50_50", new ColorDrawable(Color.WHITE), null, 0);
            //receiver.
            //chats.get(position).photo.photo_small
            //holder.photo.setImageBitmap();
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        static class ChatsHolder extends RecyclerView.ViewHolder {

            ImageView photo = null;
            TextView title = null;
            ImageReceiver receiver = null;

            ChatsHolder(View itemView) {
                super(itemView);
                photo = itemView.findViewById(R.id.photo);
                title = itemView.findViewById(R.id.title);
                receiver = new ImageReceiver(photo);
                photo.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if (photo.getDrawable() == null ||
                               ! ((BitmapDrawable) photo.getDrawable()).getBitmap().equals(receiver.getBitmap())) {
                            ((ImageView) v).setImageBitmap(receiver.getBitmap());
                        }

                    }
                });
            }

        }
    }
}
