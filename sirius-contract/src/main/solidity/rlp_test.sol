pragma solidity ^0.5.1;

import "./lib/rlp_decoder.sol";
import "./lib/rlp_encoder.sol";
import "./lib/log_util.sol";
import "./lib/byte_util.sol";
import "./lib/model.sol";

interface rlp_test_interface {
    function testUint(uint data) external returns (uint);
    function testString() external;
    function testStruct() external;
    function testMap(uint data) external;
    function testBase58() external;
    function testHubRootEncode() external;
    function testHubRootDeconde() external;
    function testWithdrawalInfoDecode(bytes calldata data) external;
    function testWithdrawalInfoEncode() external;
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

    function testHubRootEncode() external {
        // eon:18067911 offset:0 allotment:13789592 direction:ROOT
        // left:123a1d14be2941b9692aaf935e49294d9e7af3849521f5f522628c244de06f38
        // right:8d0da8cbfc71a73b24e599088e31641d292d6e6aba69aa0e3bb328fcf10659a4

        ModelLib.HubRoot memory root;
        root.eon = 18067911;
        root.node.offset = 0;
        root.node.allotment = 13789592;
        root.node.direction = ModelLib.Direction.DIRECTION_ROOT;
        root.node.nodeHash = 0x123a1d14be2941b9692aaf935e49294d9e7af3849521f5f522628c244de06f38;

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
                RLPLib.Iterator memory it1 = RLPDecoder.iterator(r);
                uint a = RLPDecoder.items(r);
                Log.log("len", a);
                uint idx1;
                while (RLPDecoder.hasNext(it1)) {
                    RLPLib.RLPItem memory r1 = RLPDecoder.next(it1);
                    if (idx1 == 0) {
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

    function testWithdrawalInfoDecode(bytes calldata data) external {
        //635b8caddb17d45161b53c0ad1788e6bee55681a:2068810:1ae52c306ff96dc222a30a22e48ba584d37de9f115b8639f14050266344c02d1:1667105:644685:6463233:4814742:42dfa8d8408995602a07e86b3945c19a7559f40a78d2ed70d7b0210775127754:30460221008ad921ea2a20bc934ccc1f4b35cb904b8cf3e9e5949cbeabc4cf14b973e226e0022100f1b9303be486ae8f418a542f71a3f12dfd2d8ae08ead9342c1fe3e4428f29d70:3046022100c12c2fd768dd6519f0bd77716ea7fa26aecc0bd2742ae92a850f2b45e638617f022100e8c8d8f2e7ece09f3115ee936f67cb929be7ebaf63e220044c3579ccf8988bab:0:9693869:13361879:13361879:0
        ModelLib.WithdrawalInfo memory w = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(data, true));

        Log.log("amount", w.amount);
    }

    function testWithdrawalInfoEncode() external {
        //0eadea397f86e28e38231aee2f7d4d5932ee1fb4:1090934:203b6514c7b7bb43ac20728a1005507c359e1e2a1adca3cc68205913bc43f95c:488065:7208584:1735212:9376331:143ff024d1f36f32fa20c5adbdbaf37fdd7b1c469cbc9e1baeabb89904a5fc76:304502203f2ffb43133aa283f7d217a14994334cdf3c3e9ab45bf0b26e6502e2a6f9fb23022100bad92bb59cec2b33b9f1d09ea321e24d7e2f8b758c7335734ffdd95444891465:30450221008d207b259d43c9edbac5be104ee2d2adf3f8ee94598b8d26e510919958a6572502207d665b6c9ec134d6fbb430df5ca12872f27a2a904108a7309dad0e754cfcd283:2:18070843:21311401:21311401:0
        //f9045d940eadea397f86e28e38231aee2f7d4d5932ee1fb4f904448310a576f8f4f8e7a0203b6514c7b7bb43ac20728a1005507c359e1e2a1adca3cc68205913bc43f95cf8c4f183077281836dfe88831a7a2c838f124ba0143ff024d1f36f32fa20c5adbdbaf37fdd7b1c469cbc9e1baeabb89904a5fc76b847304502203f2ffb43133aa283f7d217a14994334cdf3c3e9ab45bf0b26e6502e2a6f9fb23022100bad92bb59cec2b33b9f1d09ea321e24d7e2f8b758c7335734ffdd95444891465 b847  30450221008d207b259d43c9edbac5be104ee2d2adf3f8ee94598b8d26e510919958a6572502207d665b6c9ec134d6fbb430df5ca12872f27a2a904108a7309dad0e754cfcd28302840113bd3b8401452fa9f90347f854f847a0163b108422358a1f494d2103f7d54871951bde2e5282c20517c7fec305f3722d8401220a55a0159fddaf2b5e907e8a72f7e4a75286acfbcccc7eabc213b4740466e4fbd3330102840123612e84014427a7f852f846a0a75ee08b617e14b921971e78e6292da981efc3aac8f31679dd67e1b451c805678361d004a0b88021cbab59ca646d2acf9bae299ed752aa3c751f8667c99df73dcd5d364d82028401406b6883fe4e4df851f846a0aa763d6d49f2064559f5e383160ea541ba66f4576ba311da9c90edda9ebaa83383944e9aa079ec8b7643c0bbc1fa4712663314584da0431ba5493798ec5b5eb08e9e5e99690283be329a8354c096f851f846a069eadb549f16ad13fa28c9169437aecac0ea41be7e75e757ad15b7d5af9e0a1d83e9a802a0265a47ff42db64abf72745e9f42f0ba1850a2ca6c7acfa0aa1e5605f91aba6130183b5485583bcc8e2f853f846a0f6bf826452c0f0bf0c6e176a1f0cf32f28255ccaf0187b63d4eb5ca1af104f2b8306e556a0dc74e024b37f2b2dafdb6a38ce4604aa8e0c37c69e8423707e5a41fd60d81f2402840136eaee840106cefef851f846a02e36e332ba83da885bb4b366dd942ccc64bac58d234656ba2ad595f9f20570f183f1ec49a0fe9df26bd9309d8cb4b21b2671c75c2072c684eebfcd7e1c37949695c2213e9c0283e4a81c838e3127f852f847a07a3f37767e9e42ecfae5c833cbc81faf0fdc7b61de3b47e2d91f573ff71c254384012c34cca0fc2c91d7da07fa8c66679f88558ded08b9ecf5f003d3833422106cebb47189130183ab6fd983313391f852f847a01a134abd98e2c64d018cfc7a0538aed7ab76eb51ce3c3e39eee2c0f18701a5ba84010a4029a0b9809499355b4b5294ae3f498f2808449251a4ebf4faf81a2ec32bdb9580eeb10283388da18329be9ef852f847a0fb2fd567e2cf8938911c6a40dd2c80165e045c365e9471cfefb42c803cf7d653840102bf46a094e750e7d8c4b276113b072d7bb4b8e8eea319a6936a29611806de51ffa8bb340183c881008386a3a5f851f846a004cf82d42163473909404c314b0d1e2638d90c92a19dbef07334216aeeea6290835704cfa06b6151b5d71844a3d6efb3342f65aec8c3889248783f412115216afe484c6d82008309a24083eea56680

        RLPLib.Iterator memory it = RLPDecoder.iterator(RLPDecoder.toRLPItem(hex"f9045d940eadea397f86e28e38231aee2f7d4d5932ee1fb4f904448310a576f8f4f8e7a0203b6514c7b7bb43ac20728a1005507c359e1e2a1adca3cc68205913bc43f95cf8c4f183077281836dfe88831a7a2c838f124ba0143ff024d1f36f32fa20c5adbdbaf37fdd7b1c469cbc9e1baeabb89904a5fc76b847304502203f2ffb43133aa283f7d217a14994334cdf3c3e9ab45bf0b26e6502e2a6f9fb23022100bad92bb59cec2b33b9f1d09ea321e24d7e2f8b758c7335734ffdd95444891465b84730450221008d207b259d43c9edbac5be104ee2d2adf3f8ee94598b8d26e510919958a6572502207d665b6c9ec134d6fbb430df5ca12872f27a2a904108a7309dad0e754cfcd28302840113bd3b8401452fa9f90347f854f847a0163b108422358a1f494d2103f7d54871951bde2e5282c20517c7fec305f3722d8401220a55a0159fddaf2b5e907e8a72f7e4a75286acfbcccc7eabc213b4740466e4fbd3330102840123612e84014427a7f852f846a0a75ee08b617e14b921971e78e6292da981efc3aac8f31679dd67e1b451c805678361d004a0b88021cbab59ca646d2acf9bae299ed752aa3c751f8667c99df73dcd5d364d82028401406b6883fe4e4df851f846a0aa763d6d49f2064559f5e383160ea541ba66f4576ba311da9c90edda9ebaa83383944e9aa079ec8b7643c0bbc1fa4712663314584da0431ba5493798ec5b5eb08e9e5e99690283be329a8354c096f851f846a069eadb549f16ad13fa28c9169437aecac0ea41be7e75e757ad15b7d5af9e0a1d83e9a802a0265a47ff42db64abf72745e9f42f0ba1850a2ca6c7acfa0aa1e5605f91aba6130183b5485583bcc8e2f853f846a0f6bf826452c0f0bf0c6e176a1f0cf32f28255ccaf0187b63d4eb5ca1af104f2b8306e556a0dc74e024b37f2b2dafdb6a38ce4604aa8e0c37c69e8423707e5a41fd60d81f2402840136eaee840106cefef851f846a02e36e332ba83da885bb4b366dd942ccc64bac58d234656ba2ad595f9f20570f183f1ec49a0fe9df26bd9309d8cb4b21b2671c75c2072c684eebfcd7e1c37949695c2213e9c0283e4a81c838e3127f852f847a07a3f37767e9e42ecfae5c833cbc81faf0fdc7b61de3b47e2d91f573ff71c254384012c34cca0fc2c91d7da07fa8c66679f88558ded08b9ecf5f003d3833422106cebb47189130183ab6fd983313391f852f847a01a134abd98e2c64d018cfc7a0538aed7ab76eb51ce3c3e39eee2c0f18701a5ba84010a4029a0b9809499355b4b5294ae3f498f2808449251a4ebf4faf81a2ec32bdb9580eeb10283388da18329be9ef852f847a0fb2fd567e2cf8938911c6a40dd2c80165e045c365e9471cfefb42c803cf7d653840102bf46a094e750e7d8c4b276113b072d7bb4b8e8eea319a6936a29611806de51ffa8bb340183c881008386a3a5f851f846a004cf82d42163473909404c314b0d1e2638d90c92a19dbef07334216aeeea6290835704cfa06b6151b5d71844a3d6efb3342f65aec8c3889248783f412115216afe484c6d82008309a24083eea56680"));
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                address addr = RLPDecoder.toAddress(r);
                Log.log("addr", addr);
            } else if(idx == 1) {
                RLPLib.Iterator memory it1 = RLPDecoder.iterator(r);
                uint idx1;
                while(RLPDecoder.hasNext(it1)) {
                    RLPLib.RLPItem memory r1 = RLPDecoder.next(it1);
                    if(idx1 == 0) {
                        uint eon = RLPDecoder.toUint(r1);
                        Log.log("eon",eon);
                    } else if(idx1 == 1) {
                        RLPLib.Iterator memory it3 = RLPDecoder.iterator(r1);
                        uint idx3;
                        while (RLPDecoder.hasNext(it3)) {
                            RLPLib.RLPItem memory r3 = RLPDecoder.next(it3);
                            if (idx3 == 0) {
                                RLPLib.Iterator memory it4 = RLPDecoder.iterator(r3);
                                uint idx4;
                                while (RLPDecoder.hasNext(it4)) {
                                    RLPLib.RLPItem memory r4 = RLPDecoder.next(it4);
                                    if (idx4 == 0) {
                                        bytes32 hash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r4));
                                        Log.log("addressHash", hash);
                                    } else if (idx4 == 1) {
                                        RLPLib.Iterator memory it5 = RLPDecoder.iterator(r4);
                                        uint idx5;
                                        while (RLPDecoder.hasNext(it5)) {
                                            RLPLib.RLPItem memory r5 = RLPDecoder.next(it5);
                                            if (idx5 == 0) {
                                                RLPLib.Iterator memory it6 = RLPDecoder.iterator(r5);
                                                uint idx6;
                                                while (RLPDecoder.hasNext(it6)) {
                                                    RLPLib.RLPItem memory r6 = RLPDecoder.next(it6);
                                                    if (idx6 == 0) {
                                                        uint eon2 = RLPDecoder.toUint(r6);
                                                        Log.log("eon2", eon2);
                                                    } else if (idx6 == 1) {
                                                        uint version = RLPDecoder.toUint(r6);
                                                        Log.log("version", version);
                                                    } else if (idx6 == 2) {
                                                        uint sendAmount = RLPDecoder.toUint(r6);
                                                        Log.log("sendAmount", sendAmount);
                                                    } else if (idx6 == 3) {
                                                        uint receiveAmount = RLPDecoder.toUint(r6);
                                                        Log.log("receiveAmount", receiveAmount);
                                                    } else if (idx6 == 4) {
                                                        bytes32 root = ByteUtilLib.bytesToBytes32(RLPLib.toData(r6));
                                                        Log.log("root", root);
                                                    } else {}

                                                    idx6++;
                                                }
                                            } else if (idx5 == 1) {
                                                Log.log("isData", RLPLib.isData(r5));
                                                Log.log("isList", RLPDecoder.isList(r5));

                                                bytes memory sign = RLPLib.toData(r5);
                                                Log.log("sign", sign);
                                            } else if (idx5 == 2) {
                                                Log.log("isData", RLPLib.isData(r5));
                                                Log.log("isList", RLPDecoder.isList(r5));

                                                bytes memory hubSign = RLPLib.toData(r5);
                                                Log.log("hubSign", hubSign);
                                            } else {}

                                            idx5++;
                                        }
                                    } else {}

                                    idx4++;
                                }
                            } else if (idx3 == 1) {
                                ModelLib.unmarshalDirection(r3);
                            } else if (idx3 == 2) {
                                uint offset = RLPDecoder.toUint(r3);
                                Log.log("offset", offset);
                            } else if (idx3 == 3) {
                                uint allotment = RLPDecoder.toUint(r3);
                                Log.log("allotment", allotment);
                            } else {}

                            idx3++;
                        }
                    } else if(idx1 == 2) {
                        uint len = RLPDecoder.items(r1);
                        ModelLib.AMTreePathNode[] memory tmp = new ModelLib.AMTreePathNode[](len);
                        RLPLib.Iterator memory it2 = RLPDecoder.iterator(r1);
                        uint i;
                        while(RLPDecoder.hasNext(it2)) {
                            RLPLib.RLPItem memory t2 = RLPDecoder.next(it2);
                            tmp[i] = ModelLib.unmarshalAMTreePathNode(t2);
                            i++;
                        }
                        Log.log("len", len);
                    } else {}

                    idx1++;
                }
            }else if(idx == 2) {
                uint amount = RLPDecoder.toUint(r);
                Log.log("amount", amount);
            }else {}

            idx++;
        }
    }
}