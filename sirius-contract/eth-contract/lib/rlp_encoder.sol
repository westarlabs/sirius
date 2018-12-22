pragma solidity ^0.5.1;

import "./byte_util.sol";

//type to byte
library RLPEncoder {

    /**
     * @dev RLP encodes a byte string.
     * @param self The byte string to encode.
     * @return The RLP encoded string in bytes.
     */
    function encodeBytes(bytes memory self) internal pure returns (bytes memory encoded) {
        bool flag = false;
        if (self.length == 1) {
            uint8 tmp = uint8(self[0]);
            if(tmp <= 128) {
                flag = true;
            }
        }

        if(flag) {
            encoded = self;
        } else {
            encoded = concat(encodeLength(self.length, 128), self);
        }
        return encoded;
    }

    /**
     * @dev RLP encodes a list of RLP encoded byte byte strings.
     * @param self The list of RLP encoded byte strings.
     * @return The RLP encoded list of items in bytes.
     */
    function encodeList(bytes[] memory self) internal pure returns (bytes memory encoded) {
        bytes memory list = flatten(self);
        return concat(encodeLength(list.length, 192), list);
    }

    /**
     * @dev RLP encodes a list of RLP encoded byte byte strings.
     * @param self The list of RLP encoded byte strings.
     * @return The RLP encoded list of items in bytes.
     */
    function encodeList(bytes memory self) internal pure returns (bytes memory encoded) {
        return concat(encodeLength(self.length, 192), self);
    }

    /**
     * @dev RLP encodes a string.
     * @param self The string to encode.
     * @return The RLP encoded string in bytes.
     */
    function encodeString(string memory self) internal pure returns (bytes memory encoded) {
        return encodeBytes(ByteUtilLib.string2byte(self));
    }

    /**
     * @dev RLP encodes an address.
     * @param self The address to encode.
     * @return The RLP encoded address in bytes.
     */
    function encodeAddress(address self) internal pure returns (bytes memory encoded) {
        return encodeBytes(ByteUtilLib.address2byte(self));
    }

    /**
     * @dev RLP encodes a uint.
     * @param self The uint to encode.
     * @return The RLP encoded uint in bytes.
     */
    function encodeUint(uint self) internal pure returns (bytes  memory encoded) {
        return encodeBytes(ByteUtilLib.uint2byte(self));
    }

    /**
     * @dev RLP encodes an int.
     * @param self The int to encode.
     * @return The RLP encoded int in bytes.
     */
    function encodeInt(int self) internal pure returns (bytes  memory encoded) {
        return encodeBytes(ByteUtilLib.int2byte(self));
    }

    /**
     * @dev RLP encodes a bool.
     * @param self The bool to encode.
     * @return The RLP encoded bool in bytes.
     */
    function encodeBool(bool self) internal pure returns (bytes memory encoded) {
        return ByteUtilLib.bool2byte(self);
    }

    /*
     * Private functions
     */

    /**
     * @dev Encode the first byte, followed by the `len` in binary form if `length` is more than 55.
     * @param len The length of the string or the payload.
     * @param offset 128 if item is string, 192 if item is list.
     * @return RLP encoded bytes.
     */
    function encodeLength(uint len, uint offset) private pure returns (bytes memory encoded) {
        if (len < 56) {
            encoded = new bytes(1);
            uint count = len + offset;
            bytes32 tmp = bytes32(count);
            encoded[0] = tmp[31];
        } else {
            uint lenLen;
            uint i = 1;
            while (len / i != 0) {
                lenLen++;
                i *= 256;
            }

            encoded = new bytes(lenLen + 1);
            uint count = lenLen + offset + 55;
            bytes32 tmp = bytes32(count);
            encoded[0] = tmp[31];
            for(i = 1; i <= lenLen; i++) {
                count = (len / (256**(lenLen-i))) % 256;
                tmp = bytes32(count);
                encoded[i] = tmp[31];
            }
        }
        return encoded;
    }

    /**
     * @dev Flattens a list of byte strings into one byte string.
     * @notice From: https://github.com/sammayo/solidity-rlp-encoder/blob/master/RLPEncode.sol.
     * @param _list List of byte strings to flatten.
     * @return The flattened byte string.
     */
    function flatten(bytes[] memory _list) private pure returns (bytes memory encoded) {
        if (_list.length == 0) {
            return new bytes(0);
        }

        uint len;
        uint i = 0;
        for (; i < _list.length; i++) {
            len += _list[i].length;
        }

        bytes memory flattened = new bytes(len);
        uint flattenedPtr;
        assembly { flattenedPtr := add(flattened, 0x20) }

        for(i = 0; i < _list.length; i++) {
            bytes memory item = _list[i];

            uint listPtr;
            assembly { listPtr := add(item, 0x20)}

            ByteUtilLib.memcpy(flattenedPtr, listPtr, item.length);
            flattenedPtr += _list[i].length;
        }

        return flattened;
    }

    /**
     * @dev Concatenates two bytes.
     * @notice From: https://github.com/GNSPS/solidity-bytes-utils/blob/master/contracts/BytesLib.sol.
     * @param _preBytes First byte string.
     * @param _postBytes Second byte string.
     * @return Both byte string combined.
     */
    function concat(bytes memory _preBytes, bytes memory _postBytes) private pure returns (bytes memory tempBytes) {
        assembly {
            tempBytes := mload(0x40)

            let length := mload(_preBytes)
            mstore(tempBytes, length)

            let mc := add(tempBytes, 0x20)
            let end := add(mc, length)

            for {
                let cc := add(_preBytes, 0x20)
            } lt(mc, end) {
                mc := add(mc, 0x20)
                cc := add(cc, 0x20)
            } {
                mstore(mc, mload(cc))
            }

            length := mload(_postBytes)
            mstore(tempBytes, add(length, mload(tempBytes)))

            mc := end
            end := add(mc, length)

            for {
                let cc := add(_postBytes, 0x20)
            } lt(mc, end) {
                mc := add(mc, 0x20)
                cc := add(cc, 0x20)
            } {
                mstore(mc, mload(cc))
            }

            mstore(0x40, and(
            add(add(end, iszero(add(length, mload(_preBytes)))), 31),
            not(31)
            ))
        }

        return tempBytes;
    }

    function object2byte(RLPLib.RLPItem[] memory rlps) internal pure returns (bytes memory data) {
        require(rlps.length > 0);
        for(uint i=0;i<rlps.length;i++) {
            data = ByteUtilLib.append(data, encodeBytes(RLPLib.toData(rlps[i])));
        }

        data = encodeList(data);
    }

    /// @dev Return the RLP encoded bytes.
    /// @param self The RLPItem.
    /// @return The bytes.
    function toBytes(RLPLib.RLPItem memory self) internal pure returns (bytes memory bts) {
        uint len = self._unsafe_length;
        if (len == 0)
            return new bytes(0);
        bts = ByteUtilLib._copyToBytes(self._unsafe_memPtr, len);
    }
}