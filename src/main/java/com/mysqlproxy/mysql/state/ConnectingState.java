package com.mysqlproxy.mysql.state;

import com.mysqlproxy.mysql.MysqlConnection;
import com.mysqlproxy.mysql.handler.backend.BackendConnectingStateHandler;
import com.mysqlproxy.mysql.handler.frontend.FrontendConnectingStateHandler;
import com.mysqlproxy.mysql.protocol.InitialHandshakeV10Packet;

/**
 * Created by ynfeng on 2017/5/12.
 */
public class ConnectingState implements MysqlConnectionState {
    public static final ConnectingState INSTANCE = new ConnectingState();


    @Override
    public void backendHandle(MysqlConnection connection, Object o) {
        BackendConnectingStateHandler.INSTANCE.handle(connection,(InitialHandshakeV10Packet) o);
    }

    @Override
    public void frontendHandle(MysqlConnection connection, Object o) {
        FrontendConnectingStateHandler.INSTANCE.handle(connection,o);
    }
}
