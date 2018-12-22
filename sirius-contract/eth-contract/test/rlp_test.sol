pragma solidity ^0.5.1;

import "../lib/rlp_decoder.sol";
import "../lib/rlp_encoder.sol";
import "../lib/log_util.sol";
import "../lib/byte_util.sol";

interface rlp_test_interface {
    function testUint(uint data) external returns (uint);
    // function testUint2(uint data) external;
    function testString() external;
    function testStruct() external;
    function testMap(uint data) external;
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
}