package com.jp.whatsappclone.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.jp.whatsappclone.data.local.ConversationDao
import com.jp.whatsappclone.data.local.ConversationStateDao
import com.jp.whatsappclone.data.local.DraftDao
import com.jp.whatsappclone.data.local.InboxDao
import com.jp.whatsappclone.data.local.InnovfixDatabase
import com.jp.whatsappclone.data.local.LocalAccountDao
import com.jp.whatsappclone.data.local.MemberDao
import com.jp.whatsappclone.data.local.MessageDao
import com.jp.whatsappclone.data.local.OutboxDao
import com.jp.whatsappclone.data.local.ReactionDao
import com.jp.whatsappclone.data.local.ReceiptDao
import com.jp.whatsappclone.data.local.SearchDao
import com.jp.whatsappclone.data.remote.AndroidNetworkMonitor
import com.jp.whatsappclone.data.remote.AppBackendConfiguration
import com.jp.whatsappclone.data.remote.BackendConfiguration
import com.jp.whatsappclone.data.remote.NetworkMonitor
import com.jp.whatsappclone.data.repository.AuthRepository
import com.jp.whatsappclone.data.repository.ConversationRepository
import com.jp.whatsappclone.data.repository.DirectoryRepository
import com.jp.whatsappclone.data.repository.FirebaseAuthRepository
import com.jp.whatsappclone.data.repository.FirebaseConversationRepository
import com.jp.whatsappclone.data.repository.FirebaseDirectoryRepository
import com.jp.whatsappclone.data.repository.FirebaseMessagingRepository
import com.jp.whatsappclone.data.repository.MessagingRepository
import com.jp.whatsappclone.data.repository.RoomSearchRepository
import com.jp.whatsappclone.data.repository.SearchRepository
import com.jp.whatsappclone.data.sync.OutboxProcessor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InnovfixDatabase =
        Room.databaseBuilder(context, InnovfixDatabase::class.java, InnovfixDatabase.FILE_NAME)
            .addMigrations(
                InnovfixDatabase.MIGRATION_1_2,
                InnovfixDatabase.MIGRATION_2_3,
                InnovfixDatabase.MIGRATION_3_4,
            )
            .build()

    @Provides fun localAccountDao(database: InnovfixDatabase): LocalAccountDao = database.localAccountDao()
    @Provides fun memberDao(database: InnovfixDatabase): MemberDao = database.memberDao()
    @Provides fun conversationDao(database: InnovfixDatabase): ConversationDao = database.conversationDao()
    @Provides fun inboxDao(database: InnovfixDatabase): InboxDao = database.inboxDao()
    @Provides fun messageDao(database: InnovfixDatabase): MessageDao = database.messageDao()
    @Provides fun reactionDao(database: InnovfixDatabase): ReactionDao = database.reactionDao()
    @Provides fun receiptDao(database: InnovfixDatabase): ReceiptDao = database.receiptDao()
    @Provides fun outboxDao(database: InnovfixDatabase): OutboxDao = database.outboxDao()
    @Provides fun draftDao(database: InnovfixDatabase): DraftDao = database.draftDao()
    @Provides fun conversationStateDao(database: InnovfixDatabase): ConversationStateDao = database.conversationStateDao()
    @Provides fun searchDao(database: InnovfixDatabase): SearchDao = database.searchDao()

    @Provides
    @Singleton
    fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds abstract fun backendConfiguration(impl: AppBackendConfiguration): BackendConfiguration
    @Binds abstract fun networkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
    @Binds abstract fun authRepository(impl: FirebaseAuthRepository): AuthRepository
    @Binds abstract fun directoryRepository(impl: FirebaseDirectoryRepository): DirectoryRepository
    @Binds abstract fun conversationRepository(impl: FirebaseConversationRepository): ConversationRepository
    @Binds abstract fun messagingRepository(impl: FirebaseMessagingRepository): MessagingRepository
    @Binds abstract fun searchRepository(impl: RoomSearchRepository): SearchRepository
    @Binds abstract fun outboxProcessor(impl: FirebaseMessagingRepository): OutboxProcessor
}
