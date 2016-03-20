package com.example.mrokade.asanaalarm;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;


/**
 * Created by mrokade on 3/10/16.
 */
@Table(database = TaskDatabase.class)
public class Task extends BaseModel {

    @Column
    @PrimaryKey
    long id;

    @Column
    String name;

    @Column
    Date dueDate;

    public void setName(String name) {
        this.name = name;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

}