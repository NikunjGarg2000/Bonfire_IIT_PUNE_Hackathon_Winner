package com.example.bonfire;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bonfire.Adapters.allfrnds_adapter;
import com.example.bonfire.Adapters.onlinefrnds_adapter;
import com.example.bonfire.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

class onlinefrnds_Model{

    private DatabaseReference mref= FirebaseDatabase.getInstance().getReference("CurrentMeeting");
    private FirebaseAuth mauth;

    public void work(){
        mauth=FirebaseAuth.getInstance();
        mref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.hasChild("User1")){
                    mref.child("User1").setValue("User1");
                }
                if(snapshot.child("User1").getValue().equals(mauth.getCurrentUser().getDisplayName()) ||
                snapshot.child("User2").getValue().equals(mauth.getCurrentUser().getDisplayName())){

                }


                else{}
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

    }

            private com.example.bonfire.Adapters.onlinefrnds_adapter  onlineAdapter;
            private ArrayList<String> names;
            private ArrayList<String> images;


    public onlinefrnds_Model(String imageUri,String name){
        this.imageUri=imageUri;
        this.name=name;
        names=new ArrayList<String>();
        images=new ArrayList<String>();
        names.add("Lara");
        com.example.bonfire.Adapters.allfrnds_adapter adapter;
    }

    public String getImageUri() {
        return imageUri;
    }


    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String imageUri;
    String name;
}