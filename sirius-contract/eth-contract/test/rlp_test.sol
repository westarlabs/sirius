pragma solidity ^0.5.1;

import "../lib/rlp_decoder.sol";
import "../lib/rlp_encoder.sol";
import "../lib/log.sol";

interface rlp_test_interface {
    function testUint(uint data) external returns (uint);
    function testString() external;
    function testStruct() external;
    function testMap(uint data) external;
}

contract rlp_test is rlp_test_interface {
    using RLPDecoder for RLPDecoder.RLPItem;
    using RLPDecoder for RLPDecoder.Iterator;
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
        RLPDecoder.RLPItem memory item = encoded.toRLPItem(true);
        uint tmp = item.toUint();
        Log.log("testUint:encoded:", tmp);
        return (tmp);
    }

    function testString() external {
        string memory data = "abcd";
        Log.log("testString:start:", data);
        bytes memory encoded = RLPEncoder.encodeString(data);
        RLPDecoder.RLPItem memory item = encoded.toRLPItem(true);
        string memory tmp = item.toAscii();
        Log.log("testString:encoded:", tmp);
    }

    function testStruct() external {
        Test2 memory t2;
        t2.d = 100;
        bytes memory db = RLPEncoder.encodeUint(t2.d);
        t2.e = "test_struct";
        uint dbLen = db.length;
        bytes memory eb = RLPEncoder.encodeString(t2.e);
        uint ebLen = eb.length;

        Test1 memory t1;
        t1.a = 1000473892;
        bytes memory ab = RLPEncoder.encodeUint(t1.a);
        uint abLen = ab.length;
        t1.b = msg.sender;
        bytes memory bb = RLPEncoder.encodeAddress(t1.b);
        uint bbLen = bb.length;
        t1.c = t2;

        bytes[4] memory test;
        test[0] = ab;
        test[1] = bb;
        test[2] = db;
        test[3] = eb;

        bytes memory flattened = new bytes(abLen + bbLen + dbLen + ebLen);
        uint flattenedPtr;
        assembly { flattenedPtr := add(flattened, 0x20) }

        for(uint i=0; i<test.length; i++) {
            bytes memory item = test[i];

            uint selfPtr;
            assembly { selfPtr := add(item, 0x20)}

            RLPEncoder.memcpy(flattenedPtr, selfPtr, item.length);
            flattenedPtr += test[i].length;
        }

        bytes memory encoded = RLPEncoder.encodeList(flattened);

        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(encoded);

        RLPDecoder.Iterator memory it = rlp.iterator();
        uint idx;
        while(it.hasNext()) {
            RLPDecoder.RLPItem memory r = it.next();
            if( idx == 0 ) Log.log("testStruct:1:", r.toUint());
            else if ( idx == 1 ) Log.log("testStruct:2:", r.toAddress());
            else if ( idx == 2 ) Log.log("testStruct:3:", r.toUint());
            else if ( idx == 3 ) Log.log("testStruct:4:", r.toAscii());

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
        uint e1kbLen = e1kb.length;
        bytes memory e1vb = RLPEncoder.encodeUint(e1.value);
        uint e1vbLen = e1vb.length;

        bytes memory e2kb = RLPEncoder.encodeString(e2.key);
        uint e2kbLen = e2kb.length;
        bytes memory e2vb = RLPEncoder.encodeUint(e2.value);
        uint e2vbLen = e2vb.length;

        bytes[4] memory kvs;
        kvs[0] = e1kb;
        kvs[1] = e1vb;
        kvs[2] = e2kb;
        kvs[3] = e2vb;

        bytes memory flattened = new bytes(e1kbLen + e1vbLen + e2kbLen + e2vbLen);
        uint flattenedPtr;
        assembly { flattenedPtr := add(flattened, 0x20) }

        for(uint i=0; i<kvs.length; i++) {
            bytes memory item = kvs[i];

            uint selfPtr;
            assembly { selfPtr := add(item, 0x20)}

            RLPEncoder.memcpy(flattenedPtr, selfPtr, item.length);
            flattenedPtr += kvs[i].length;
        }

        bytes memory encoded = RLPEncoder.encodeList(flattened);

        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(encoded);

        uint i;
        uint listLen = RLPDecoder.items(rlp);
        Log.log("testMap:0:", listLen);
        RLPDecoder.Iterator memory it = rlp.iterator();
        while(it.hasNext() && i < listLen) {
            RLPDecoder.RLPItem memory si = it.next();
            if((i%2) == 0) {
                Log.log("testMap:1:", si.toAscii());
            } else {
                Log.log("testMap:1:", si.toUint());
            }
            i++;
        }
    }
}