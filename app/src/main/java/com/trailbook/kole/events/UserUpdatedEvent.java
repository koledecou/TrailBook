package com.trailbook.kole.events;

import com.trailbook.kole.data.User;

/**
 * Created by kole on 12/5/2014.
 */
public class UserUpdatedEvent {
    User mUser;
    public UserUpdatedEvent(User user) {
        mUser = user;
    }

    public User getUser() {
        return mUser;
    }
}
