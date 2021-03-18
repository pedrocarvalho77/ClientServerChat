if(!s.equals("/priv") || !searchNick(sockets,cutMessage(1,message))) {       
    sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
}
else {
    si = sockets.remove(tmp);
    if (si.state==3) {
        sockets.add(si);
        int tmp = searchSocket(sockets, cutMessage(1,message));
        for(int i=0; i<tmp;i++){
            si = l.get(i);
            SocketChannel sc = si.sc;
        }
        sc.write(enc.encode(CharBuffer.wrap("OK\n")));
        sc.write(enc.encode(CharBuffer.wrap(message)));
        //messageRoom(sockets,"MESSAGE " + si.nick + " " + message + "\n", si.room);
    }
    else {
    sockets.add(si);
    sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
    }
}