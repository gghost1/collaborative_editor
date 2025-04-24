import { Client, Message } from '@stomp/stompjs';

declare module '@stomp/stompjs' {
    interface IMessage extends Message {
        binaryBody: Uint8Array;
        body: string;
    }
}