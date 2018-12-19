pragma solidity ^0.5.1;

import "./lib/rlp_decoder.sol";
import "./lib/rlp_encoder.sol";

contract TestRLP {

    using RLPDecoder for RLPDecoder.RLPItem;
    using RLPDecoder for RLPDecoder.Iterator;
    using RLPDecoder for bytes;

    event NewData(bool, byte, int, string);

    struct Data {
        bool boolValue;
        int intValue;
        string stringValue;
        address addressValue;
    }

    function decodeData(bytes memory dataBytes) internal pure returns (Data memory){
        RLPDecoder.RLPItem[] memory rlpList = dataBytes.toRLPItem().toList();
        return Data({
            boolValue : rlpList[0].toBool(),
            intValue : rlpList[1].toInt(),
            stringValue : rlpList[2].toAscii(),
            addressValue : rlpList[3].toAddress()
            });
    }

    function encodeData(Data memory _data) internal pure returns (bytes memory){
        bytes memory boolValue = RLPEncoder.encodeBool(_data.boolValue);
        bytes memory intValue = RLPEncoder.encodeInt(_data.intValue);
        bytes memory stringValue = RLPEncoder.encodeString(_data.stringValue);
        bytes memory addressValue = RLPEncoder.encodeAddress(_data.addressValue);
        bytes memory flattened = RLPEncoder.append(RLPEncoder.append(RLPEncoder.append(boolValue, intValue), stringValue), addressValue);
        bytes memory encoded = RLPEncoder.encodeList(flattened);
        return encoded;
    }

    Data data;

    function set(bytes memory _data) public {
        data = decodeData(_data);
        //emit NewData(data.boolValue, data.byteValue, data.intValue, data.stringValue);
    }

    function get() public view returns (bytes memory){
        return encodeData(data);
    }

    function echo(bytes memory _data) public view returns (bytes memory){
        //return _data;
        return encodeData(decodeData(_data));
    }

    //TODO solidity bug, byte always 0
    function echoByte(byte b) public view returns (byte){
        return b;
    }
}