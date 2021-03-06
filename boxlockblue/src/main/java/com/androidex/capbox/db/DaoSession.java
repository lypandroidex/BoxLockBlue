package com.androidex.capbox.db;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.androidex.capbox.db.ChatRecord;
import com.androidex.capbox.db.Note;

import com.androidex.capbox.db.ChatRecordDao;
import com.androidex.capbox.db.NoteDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig chatRecordDaoConfig;
    private final DaoConfig noteDaoConfig;

    private final ChatRecordDao chatRecordDao;
    private final NoteDao noteDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        chatRecordDaoConfig = daoConfigMap.get(ChatRecordDao.class).clone();
        chatRecordDaoConfig.initIdentityScope(type);

        noteDaoConfig = daoConfigMap.get(NoteDao.class).clone();
        noteDaoConfig.initIdentityScope(type);

        chatRecordDao = new ChatRecordDao(chatRecordDaoConfig, this);
        noteDao = new NoteDao(noteDaoConfig, this);

        registerDao(ChatRecord.class, chatRecordDao);
        registerDao(Note.class, noteDao);
    }
    
    public void clear() {
        chatRecordDaoConfig.clearIdentityScope();
        noteDaoConfig.clearIdentityScope();
    }

    public ChatRecordDao getChatRecordDao() {
        return chatRecordDao;
    }

    public NoteDao getNoteDao() {
        return noteDao;
    }

}
