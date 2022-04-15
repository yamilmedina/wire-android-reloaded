package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUserDirectoryUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KaliumCoreLogic

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentAccount

@Module
@InstallIn(SingletonComponent::class)
class CoreLogicModule {

    @KaliumCoreLogic
    @Singleton
    @Provides
    fun coreLogicProvider(@ApplicationContext context: Context): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path
        val deviceLabel = DeviceLabel.label

        return CoreLogic(
            appContext = context,
            rootPath = rootPath,
            clientLabel = deviceLabel
        )
    }
}

@Module
@InstallIn(ViewModelComponent::class)
class SessionModule {
    @CurrentAccount
    @ViewModelScoped
    @Provides
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    fun currentSessionProvider(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return runBlocking {
            return@runBlocking when (val result = coreLogic.getAuthenticationScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.authSession.userId
                else -> {
                    throw IllegalStateException("no current session was found")
                }
            }
        }
    }
}

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class UseCaseModule {

    @ViewModelScoped
    @Provides
    fun loginUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().login

    @ViewModelScoped
    @Provides
    fun logoutUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @ViewModelScoped
    @Provides
    fun validateEmailUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().validateEmailUseCase

    @ViewModelScoped
    @Provides
    fun validatePasswordUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().validatePasswordUseCase

    @ViewModelScoped
    @Provides
    fun validateUserHandleUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().validateUserHandleUseCase

    @ViewModelScoped
    @Provides
    fun registerAccountUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().register.register

    @ViewModelScoped
    @Provides
    fun requestCodeUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().register.requestActivationCode

    @ViewModelScoped
    @Provides
    fun verifyCodeUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().register.activate

    @ViewModelScoped
    @Provides
    fun setUserHandleUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.setUserHandle

    @ViewModelScoped
    @Provides
    fun provideObserveConversationListDetailsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationListDetails

    @ViewModelScoped
    @Provides
    fun getServerConfigUserCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().getServerConfig

    @ViewModelScoped
    @Provides
    // TODO: kind of redundant to CurrentSession - need to rename CurrentSession
    fun currentSessionUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) = coreLogic.getAuthenticationScope().session.currentSession

    @ViewModelScoped
    @Provides
    fun selfClientsUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.selfClients

    @ViewModelScoped
    @Provides
    fun getAvatarAsset(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetAvatarAssetUseCase =
        coreLogic.getSessionScope(currentAccount).users.getPublicAsset

    @ViewModelScoped
    @Provides
    fun uploadUserAvatar(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): UploadUserAvatarUseCase =
        coreLogic.getSessionScope(currentAccount).users.uploadUserAvatar

    @ViewModelScoped
    @Provides
    fun observeConversationDetailsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationDetails

    @ViewModelScoped
    @Provides
    fun observeConversationMembersUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationMembers

    @ViewModelScoped
    @Provides
    fun getMessagesUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.getRecentMessages

    @ViewModelScoped
    @Provides
    fun deleteClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.deleteClient

    @ViewModelScoped
    @Provides
    fun registerClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.register

    @ViewModelScoped
    @Provides
    fun needsToRegisterClientUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).client.needsToRegisterClient

    @ViewModelScoped
    @Provides
    fun listenToEventsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).listenToEvents

    @ViewModelScoped
    @Provides
    fun providesGetSelfUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getSelfUser

    @ViewModelScoped
    @Provides
    fun providesSendTextMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendTextMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendTextMessage

    @ViewModelScoped
    @Provides
    fun providesSendImageMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendImageMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendImageMessage

    @ViewModelScoped
    @Provides
    fun providesSendAssetMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendAssetMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendAssetMessage

    @ViewModelScoped
    @Provides
    fun providesGetPrivateAssetUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetMessageAssetUseCase = coreLogic.getSessionScope(currentAccount).messages.getAssetMessage

    @ViewModelScoped
    @Provides
    fun providesSearchKnownUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchKnownUsersUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchKnownUsers

    @ViewModelScoped
    @Provides
    fun providesSearchPublicUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchUserDirectoryUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchUserDirectory

    @ViewModelScoped
    @Provides
    fun provideAddAuthenticatedUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic): AddAuthenticatedUserUseCase =
        coreLogic.getAuthenticationScope().addAuthenticatedAccount

    @ViewModelScoped
    @Provides
    fun providesGetAllKnownUsers(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAllKnownUsersUseCase =
        coreLogic.getSessionScope(currentAccount).users.getAllKnownUsers

    @ViewModelScoped
    @Provides
    fun providesGetKnownUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetKnownUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getKnownUser

    @ViewModelScoped
    @Provides
    fun providesDeleteMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): DeleteMessageUseCase =
        coreLogic.getSessionScope(currentAccount).messages.deleteMessage

    @ViewModelScoped
    @Provides
    fun startCallUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): StartCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.startCall

    @ViewModelScoped
    @Provides
    fun providesGetOrCreateOneToOneConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetOrCreateOneToOneConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.getOrCreateOneToOneConversationUseCase

}
