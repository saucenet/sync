package com.sauce.sync;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.sauce.sync.models.SauceEntity;
import com.sauce.sync.models.SauceUser;

/**
 * Created by sauce on 3/9/15.
 */

public class OfyService {
    static {
        factory().register(SauceUser.class);
        factory().register(SauceEntity.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}
