pragma solidity ^0.5.1;

import "../lib/rlp_decoder.sol";
import "../lib/rlp_encoder.sol";
import "../lib/log_util.sol";
import "../lib/byte_util.sol";
import "../lib/model.sol";

interface rlp_test_interface {
    function testUint(uint data) external returns (uint);
    function testString() external;
    function testStruct() external;
    function testMap(uint data) external;
    function testBase58() external;
    function testHubRootEncode() external;
    function testHubRootDeconde() external;
}

contract rlp_test is rlp_test_interface {
    using RLPLib for RLPLib.RLPItem;
    using RLPLib for RLPLib.Iterator;
    using RLPLib for bytes;
    using RLPDecoder for bytes;

    struct KV {
        string key;
        uint value;
    }

    struct Test2 {
        uint32 d;
        string e;
    }

    struct Test1 {
        uint a;
        address b;
        Test2 c;
    }

    function testUint(uint data) external returns (uint) {
        Log.log("testUint:start:", data);
        bytes memory encoded = RLPEncoder.encodeUint(data);
        Log.log("testUint:2:", encoded);
        RLPLib.RLPItem memory item = encoded.toRLPItem(true);
        uint tmp = RLPDecoder.toUint(item);
        Log.log("testUint:encoded:", tmp);
        return (tmp);
    }

    function testString() external {
        string memory data = "abcd";
        Log.log("testString:start:", data);
        bytes memory encoded = RLPEncoder.encodeString(data);
        RLPLib.RLPItem memory item = encoded.toRLPItem(true);
        string memory tmp = RLPDecoder.toAscii(item);
        Log.log("testString:encoded:", tmp);
    }

    function testStruct() external {
        Test2 memory t2;
        t2.d = 100;
        bytes memory db = RLPEncoder.encodeUint(t2.d);
        t2.e = "test_struct";
        bytes memory eb = RLPEncoder.encodeString(t2.e);

        Test1 memory t1;
        t1.a = 1000473892;
        bytes memory ab = RLPEncoder.encodeUint(t1.a);
        t1.b = msg.sender;
        bytes memory bb = RLPEncoder.encodeAddress(t1.b);
        t1.c = t2;

        bytes memory flattened = ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ab, bb), db), eb);

        // bytes memory flattened1 = RLPEncoder.encodeList(RLPEncoder.encodeBytes(RLPEncoder.append(db, eb)));
        // bytes memory flattened = RLPEncoder.encodeBytes(RLPEncoder.append(RLPEncoder.append(ab, bb), flattened1));

        bytes memory encoded = RLPEncoder.encodeList(flattened);

        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(encoded);

        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if( idx == 0 ) Log.log("testStruct:1:", RLPDecoder.toUint(r));
            else if ( idx == 1 ) Log.log("testStruct:2:", RLPDecoder.toAddress(r));
            else if ( idx == 2 ) Log.log("testStruct:3:", RLPDecoder.toUint(r));
            else if ( idx == 3 ) Log.log("testStruct:4:", RLPDecoder.toAscii(r));

            idx++;
        }
    }

    function testMap(uint data) external {
        KV memory e1;
        e1.key = "test1";
        e1.value = data;

        KV memory e2;
        e2.key = "test2";
        e2.value = data;

        bytes memory e1kb = RLPEncoder.encodeString(e1.key);
        bytes memory e1vb = RLPEncoder.encodeUint(e1.value);

        bytes memory e2kb = RLPEncoder.encodeString(e2.key);
        bytes memory e2vb = RLPEncoder.encodeUint(e2.value);

        bytes memory flattened = ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(e1kb, e1vb), e2kb), e2vb);

        bytes memory encoded = RLPEncoder.encodeList(flattened);

        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(encoded);

        uint i;
        uint listLen = RLPDecoder.items(rlp);
        Log.log("testMap:0:", listLen);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        while(RLPDecoder.hasNext(it) && i < listLen) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if((i%2) == 0) {
                Log.log("testMap:1:", RLPDecoder.toAscii(r));
            } else {
                Log.log("testMap:1:", RLPDecoder.toUint(r));
            }
            i++;
        }
    }

    function testBase58() external {
        string memory str = "6a190eef45f589373a463AFb3B90493E696c45e2";
        bytes memory tmp = Base58Util.base58String2Bytes(str);
        Log.log("base58_1", tmp);
        bytes32 tmp2 = ByteUtilLib.bytesToBytes32(tmp);
        Log.log("base58_2", tmp2);
        string memory tmp3 = Base58Util.bytes32ToBase58(tmp2);
        Log.log("base58_3", tmp3);
    }

    function testHubRootEncode() external {
        // eon:18067911 offset:0 allotment:13789592 direction:ROOT
        // left:123a1d14be2941b9692aaf935e49294d9e7af3849521f5f522628c244de06f38
        // right:8d0da8cbfc71a73b24e599088e31641d292d6e6aba69aa0e3bb328fcf10659a4

        ModelLib.HubRoot memory root;
        root.eon = 18067911;
        root.node.offset = 0;
        root.node.allotment = 13789592;
        root.node.direction = ModelLib.Direction.DIRECTION_ROOT;
        root.node.nodeInfo.left = 0x123a1d14be2941b9692aaf935e49294d9e7af3849521f5f522628c244de06f38;
        root.node.nodeInfo.offset = 0;
        root.node.nodeInfo.right = 0x8d0da8cbfc71a73b24e599088e31641d292d6e6aba69aa0e3bb328fcf10659a4;

        bytes memory data = ModelLib.marshalHubRoot(root);
        Log.log("test", data);
    }

    function testHubRootDeconde() external {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(hex"f852f84bf843a0123a1d14be2941b9692aaf935e49294d9e7af3849521f5f522628c244de06f3880a08d0da8cbfc71a73b24e599088e31641d292d6e6aba69aa0e3bb328fcf10659a4808083d26998840113b1c7", true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                // bytes memory tmp1 = RLPLib.toData(r);

                // RLPLib.RLPItem memory rlp1 = RLPDecoder.toRLPItem(tmp1, true);
                RLPLib.Iterator memory it1 = RLPDecoder.iterator(r);
                uint a = RLPDecoder.items(r);
                Log.log("len", a);
                uint idx1;
                while (RLPDecoder.hasNext(it1)) {
                    RLPLib.RLPItem memory r1 = RLPDecoder.next(it1);
                    if (idx1 == 0) {
                        // bytes memory tmp2 = RLPLib.toData(r1);

                        // RLPLib.RLPItem memory rlp2 = RLPDecoder.toRLPItem(tmp2, true);
                        RLPLib.Iterator memory it2 = RLPDecoder.iterator(r1);
                        uint idx2;
                        while (RLPDecoder.hasNext(it2)) {
                            RLPLib.RLPItem memory r2 = RLPDecoder.next(it2);
                            if (idx2 == 0) {
                                bytes32 left = ByteUtilLib.bytesToBytes32(RLPLib.toData(r2));
                                Log.log("left", left);
                            } else if (idx2 == 1) {
                                uint offset = RLPDecoder.toUint(r2);
                                Log.log("offset", offset);
                            } else if (idx2 == 2) {
                                bytes32 right = ByteUtilLib.bytesToBytes32(RLPLib.toData(r2));
                                Log.log("right", right);
                            } else {}

                            idx2++;
                        }
                    } else if (idx1 == 1) {
                        uint direction = RLPDecoder.toUint(r1);
                        Log.log("direction", direction);
                    } else if (idx1 == 2) {
                        uint offset = RLPDecoder.toUint(r1);
                        Log.log("offset", offset);
                    } else if (idx1 == 3) {
                        uint allotment = RLPDecoder.toUint(r1);
                        Log.log("allotment", allotment);
                    } else {}

                    idx1++;
                }
            } else if(idx == 1) {
                uint eon = RLPDecoder.toUint(r);
                Log.log("eon", eon);
            } else {}

            idx++;
        }
    }
}