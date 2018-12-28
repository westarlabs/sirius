pragma solidity ^0.5.1;

//byte operat lib
library ByteUtilLib {

    function equal(bytes memory one, bytes memory two) internal pure returns (bool) {
        if (!(one.length == two.length)) {
            return false;
        }
        for (uint i=0; i<one.length; i++) {
            if (!(one[i] == two[i])) {
	            return false;
            }
        }
        return true;
    }

    function bytesToBytes32(bytes memory b) internal pure returns (bytes32) {
        return bytesToBytes32(b, 0);
    }

    function bytesToBytes32(bytes memory b, uint offset) internal pure returns (bytes32) {
        bytes32 out;

        for (uint i = 0; i < 32; i++) {
            out |= bytes32(b[offset + i] & 0xFF) >> (i * 8);
        }
        return out;
    }

    function bytes32ToBytes(bytes32 data) internal pure returns (bytes memory) {
        bytes memory bs = new bytes(32);
        for (uint256 i; i < 32; i++) {
            bs[i] = data[i];
        }

        return bs;
    }

    /**
     * @dev RLP encodes a string.
     * @param self The string to encode.
     * @return The RLP encoded string in bytes.
     */
    function string2byte(string memory self) internal pure returns (bytes memory encoded) {
        return bytes(self);
    }

    /**
     * @dev RLP encodes an address.
     * @param self The address to encode.
     * @return The RLP encoded address in bytes.
     */
    function address2byte(address self) internal pure returns (bytes memory encoded) {
        assembly {
            let m := mload(0x40)
            mstore(add(m, 20), xor(0x140000000000000000000000000000000000000000, self))
            mstore(0x40, add(m, 52))
            encoded := m
        }
    }

    function bytesToAddress(bytes memory b) internal pure returns (address) {
        uint result = 0;
        for (uint i=0; i<b.length;i++) {
            uint8 c = uint8(b[i]);
            if (c >= 48 && c <= 57) {
                result = result * 16 + (c - 48);
            }
            if(c >= 65 && c<= 90) {
                result = result * 16 + (c - 55);
            }
            if(c >= 97 && c<= 122) {
                result = result * 16 + (c - 87);
            }
        }
        return address(result);
    }

    /**
     * @dev RLP encodes a uint.
     * @param self The uint to encode.
     * @return The RLP encoded uint in bytes.
     */
    function uint2byte(uint self) internal pure returns (bytes  memory encoded) {
        return toBinary(self);
    }

    /**
     * @dev RLP encodes an int.
     * @param self The int to encode.
     * @return The RLP encoded int in bytes.
     */
    function int2byte(int self) internal pure returns (bytes  memory encoded) {
        return toBinary(uint(self));
    }

    function bool2byte(bool self) internal pure returns (bytes memory data) {
        data = new bytes(1);
        //TODO ensure false should use 0x80 or 0x0
        data[0] = (self ? bytes1(0x01) : bytes1(0x0));
        return data;
    }

    /**
     * @dev Encode integer in big endian binary form with no leading zeroes.
     * @notice TODO: This should be optimized with assembly to save gas costs.
     * @param _x The integer to encode.
     * @return RLP encoded bytes.
     */
    function toBinary(uint _x) private pure returns (bytes memory encoded) {
        bytes memory b = new bytes(32);
        assembly {
            mstore(add(b, 32), _x)
        }
        uint i = 0;
        for (; i < 32; i++) {
            if (b[i] != 0) {
                break;
            }
        }
        bytes memory res = new bytes(32 - i);
        for (uint j = 0; j < res.length; j++) {
            res[j] = b[i++];
        }
        return res;
    }

    /**
     * @dev Copies a piece of memory to another location.
     * @notice From: https://github.com/Arachnid/solidity-stringutils/blob/master/src/strings.sol.
     * @param _dest Destination location.
     * @param _src Source location.
     * @param _len Length of memory to copy.
     */
    function memcpy(uint _dest, uint _src, uint _len) internal pure {
        uint dest = _dest;
        uint src = _src;
        uint len = _len;

        for(; len >= 32; len -= 32) {
            assembly {
                mstore(dest, mload(src))
            }
            dest += 32;
            src += 32;
        }

        uint mask = 256 ** (32 - len) - 1;
        assembly {
            let srcpart := and(mload(src), not(mask))
            let destpart := and(mload(dest), mask)
            mstore(dest, or(destpart, srcpart))
        }
    }

    function append(bytes memory first, bytes memory second) internal pure returns (bytes memory appended) {
        appended = new bytes(first.length + second.length);
        uint flattenedPtr;
        assembly { flattenedPtr := add(appended, 0x20) }

        uint firstPtr;
        assembly { firstPtr := add(first, 0x20)}

        memcpy(flattenedPtr, firstPtr, first.length);
        flattenedPtr += first.length;

        uint secondPtr;
        assembly { secondPtr := add(second, 0x20)}

        memcpy(flattenedPtr, secondPtr, second.length);
        flattenedPtr += second.length;

        return appended;
    }

    // Assumes that enough memory has been allocated to store in target.
    function _copyToBytes(uint btsPtr, uint len) internal pure returns(bytes memory) {
        // Exploiting the fact that 'tgt' was the last thing to be allocated,
        // we can write entire words, and just overwrite any excess.
        bytes memory tgt = new bytes(len);
        uint i = 0;
        uint words;
        uint rOffset;
        uint wOffset;
        assembly {
            words := div(add(len, 31), 32)
            rOffset := btsPtr
            wOffset := add(tgt, 0x20)
        }

        for(;i<words;i++) {
            assembly {
                let offset := mul(i, 0x20)
                mstore(add(wOffset, offset), mload(add(rOffset, offset)))
            }
        }

        assembly {
            mstore(add(tgt, add(0x20, mload(tgt))), 0)
        }

        return tgt;
    }
}

//public function
library RLPLib {

    uint constant DATA_SHORT_START = 0x80;
    uint constant DATA_LONG_START = 0xB8;

    struct RLPItem {
        uint _unsafe_memPtr;    // Pointer to the RLP-encoded bytes.
        uint _unsafe_length;    // Number of bytes. This is the full length of the string.
    }

    struct Iterator {
        RLPItem _unsafe_item;   // Item that's being iterated over.
        uint _unsafe_nextPtr;   // Position of the next item in the list.
    }

    /// @dev Check if the RLP item is data.
    /// @param self The RLP item.
    /// @return 'true' if the item is data.
    function isData(RLPLib.RLPItem memory self) internal pure returns (bool ret) {
        if (self._unsafe_length == 0)
            return false;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            ret := lt(byte(0, mload(memPtr)), 0xC0)
        }
    }

    // Get start position and length of the data.
    function _decode(RLPLib.RLPItem memory self) internal pure returns (uint memPtr, uint len) {
        if(!isData(self))
            revert();
        uint b0;
        uint start = self._unsafe_memPtr;
        assembly {
            b0 := byte(0, mload(start))
        }
        if (b0 < DATA_SHORT_START) {
            memPtr = start;
            len = 1;
            return (memPtr, len);
        }
        if (b0 < DATA_LONG_START) {
            len = self._unsafe_length - 1;
            memPtr = start + 1;
        } else {
            uint bLen;
            assembly {
                bLen := sub(b0, 0xB7) // DATA_LONG_OFFSET
            }
            len = self._unsafe_length - 1 - bLen;
            memPtr = start + bLen + 1;
        }
        return (memPtr, len);
    }

    /// @dev Decode an RLPItem into bytes. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toData(RLPLib.RLPItem memory self) internal pure returns (bytes memory bts) {
        if(!isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = _decode(self);
        bts = ByteUtilLib._copyToBytes(rStartPos, len);
    }
}
