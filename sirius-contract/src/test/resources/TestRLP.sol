pragma solidity ^0.5.1;

import "./RLP.sol";

contract TestRLP {

    using RLP for bytes;
    using RLP for bytes[];
    using RLP for RLP.RLPItem;
    using RLP for RLP.Iterator;

    //    constructor () public{
    //    }

    struct Data {
        bool boolValue;
        byte byteValue;
        int intValue;
        string stringValue;
    }

    function decodeData(bytes memory dataBytes) internal pure returns (Data memory){
        RLP.RLPItem[] memory rlpList = dataBytes.toRLPItem().toList();
        return Data({
            boolValue : rlpList[0].toBool(),
            byteValue : rlpList[1].toByte(),
            intValue : rlpList[2].toInt(),
            stringValue : rlpList[3].toAscii()
            });
    }

    function encodeData(Data memory _data) internal pure returns (bytes memory){
        return abi.encodePacked(_data.boolValue, _data.byteValue, _data.intValue, _data.stringValue);
    }

    Data data;

    function set(bytes memory _data) public {
        data = decodeData(_data);
    }

    function hash() public view returns (bool, byte, int, string memory){
        return (data.boolValue, data.byteValue, data.intValue, data.stringValue);
    }
}